import { http } from './client.js';

export const validationApi = {
  rules: (datasetId) => http.get('/api/validation/rules', { datasetId }),
  createRule: (rule) => http.post('/api/validation/rules', rule),
  updateRule: (id, patch) => http.patch(`/api/validation/rules/${id}`, patch),
  runValidation: (datasetVersionId) => http.post('/api/validation/runs', { datasetVersionId }),
  run: (id) => http.get(`/api/validation/runs/${id}`),
  runHistory: (datasetVersionId) => http.get('/api/validation/runs', { datasetVersionId }),
  runResults: (id, { page = 0, size = 20, passed, severity } = {}) =>
    http.get(`/api/validation/runs/${id}/results`, { page, size, passed, severity }),
};
