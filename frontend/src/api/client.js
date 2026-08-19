const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

const TOKEN_STORAGE_KEY = 'aedmp.auth.token';

let inMemoryToken = null;
let onUnauthorized = null;

export function setAuthToken(token) {
  inMemoryToken = token;
  if (token) {
    sessionStorage.setItem(TOKEN_STORAGE_KEY, token);
  } else {
    sessionStorage.removeItem(TOKEN_STORAGE_KEY);
  }
}

export function getAuthToken() {
  if (inMemoryToken) return inMemoryToken;
  const stored = sessionStorage.getItem(TOKEN_STORAGE_KEY);
  if (stored) inMemoryToken = stored;
  return inMemoryToken;
}

export function registerUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

export class ApiError extends Error {
  constructor(status, envelope) {
    super(envelope?.message || `Request failed with status ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.code = envelope?.code;
    this.requestId = envelope?.requestId;
    this.fieldErrors = envelope?.fieldErrors;
  }
}

export class NetworkError extends Error {
  constructor(cause) {
    super('Unable to reach the server. Check your connection and try again.');
    this.name = 'NetworkError';
    this.cause = cause;
  }
}

async function parseErrorEnvelope(response) {
  try {
    const data = await response.json();
    return data;
  } catch {
    return { code: 'UNKNOWN', message: response.statusText };
  }
}

/**
 * Shared fetch wrapper: resolves relative paths against VITE_API_BASE_URL (falls back to
 * same-origin so the Vite dev proxy for /api and /actuator can be used instead), attaches the
 * bearer token, and normalizes both HTTP-level and network-level failures.
 */
export async function apiFetch(path, options = {}) {
  const { method = 'GET', body, headers = {}, isMultipart = false, signal } = options;

  const url = path.startsWith('http') ? path : `${API_BASE_URL}${path}`;
  const token = getAuthToken();

  const finalHeaders = { ...headers };
  if (!isMultipart && body !== undefined) {
    finalHeaders['Content-Type'] = 'application/json';
  }
  if (token) {
    finalHeaders['Authorization'] = `Bearer ${token}`;
  }

  let response;
  try {
    response = await fetch(url, {
      method,
      headers: finalHeaders,
      body: body === undefined ? undefined : isMultipart ? body : JSON.stringify(body),
      signal,
    });
  } catch (cause) {
    if (cause?.name === 'AbortError') throw cause;
    throw new NetworkError(cause);
  }

  if (response.status === 204) return null;

  if (!response.ok) {
    const envelope = await parseErrorEnvelope(response);
    if (response.status === 401) {
      setAuthToken(null);
      onUnauthorized?.();
    }
    throw new ApiError(response.status, envelope);
  }

  const text = await response.text();
  if (!text) return null;
  return JSON.parse(text);
}

export function buildQuery(params = {}) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return;
    search.set(key, value);
  });
  const qs = search.toString();
  return qs ? `?${qs}` : '';
}

export const http = {
  get: (path, params) => apiFetch(`${path}${buildQuery(params)}`),
  post: (path, body) => apiFetch(path, { method: 'POST', body }),
  patch: (path, body) => apiFetch(path, { method: 'PATCH', body }),
  postMultipart: (path, formData) => apiFetch(path, { method: 'POST', body: formData, isMultipart: true }),
};
