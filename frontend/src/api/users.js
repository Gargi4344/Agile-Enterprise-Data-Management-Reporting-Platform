import { http } from './client.js';

export const usersApi = {
  list: ({ page = 0, size = 20, search } = {}) => http.get('/api/users', { page, size, search }),
  create: (user) => http.post('/api/users', user),
  update: (id, patch) => http.patch(`/api/users/${id}`, patch),
  roles: () => http.get('/api/roles'),
  permissions: () => http.get('/api/permissions'),
};
