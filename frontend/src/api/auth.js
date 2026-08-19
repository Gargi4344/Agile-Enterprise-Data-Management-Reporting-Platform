import { http } from './client.js';

export const authApi = {
  login: (email, password) => http.post('/api/auth/login', { email, password }),
  me: () => http.get('/api/auth/me'),
};
