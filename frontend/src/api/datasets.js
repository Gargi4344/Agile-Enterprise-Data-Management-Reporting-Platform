import { http } from './client.js';

export const datasetsApi = {
  list: ({ page = 0, size = 20, search } = {}) => http.get('/api/datasets', { page, size, search }),
  create: ({ name, description }) => http.post('/api/datasets', { name, description }),
  get: (id) => http.get(`/api/datasets/${id}`),
  update: (id, patch) => http.patch(`/api/datasets/${id}`, patch),
  versions: (id) => http.get(`/api/datasets/${id}/versions`),
  uploadVersion: (id, { file, sourceSystem }) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('sourceSystem', sourceSystem);
    return http.postMultipart(`/api/datasets/${id}/versions`, formData);
  },
  version: (versionId) => http.get(`/api/versions/${versionId}`),
  versionColumns: (versionId) => http.get(`/api/versions/${versionId}/columns`),
};

export function uploadVersionWithProgress(datasetId, { file, sourceSystem }, onProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    const base = import.meta.env.VITE_API_BASE_URL ?? '';
    xhr.open('POST', `${base}/api/datasets/${datasetId}/versions`);
    const token = sessionStorage.getItem('aedmp.auth.token');
    if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`);

    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) {
        onProgress?.(Math.round((event.loaded / event.total) * 100));
      }
    };
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          resolve(xhr.responseText ? JSON.parse(xhr.responseText) : null);
        } catch (e) {
          reject(e);
        }
      } else {
        let envelope;
        try {
          envelope = JSON.parse(xhr.responseText);
        } catch {
          envelope = { message: xhr.statusText };
        }
        const err = new Error(envelope.message || 'Upload failed');
        err.status = xhr.status;
        err.code = envelope.code;
        err.fieldErrors = envelope.fieldErrors;
        reject(err);
      }
    };
    xhr.onerror = () => reject(new Error('Network error during upload'));

    const formData = new FormData();
    formData.append('file', file);
    formData.append('sourceSystem', sourceSystem);
    xhr.send(formData);
  });
}
