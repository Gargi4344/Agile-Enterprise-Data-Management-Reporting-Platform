import { http } from './client.js';

export const jobsApi = {
  create: ({ datasetVersionId, jobType }) => http.post('/api/jobs', { datasetVersionId, jobType }),
  get: (id) => http.get(`/api/jobs/${id}`),
  list: ({ datasetVersionId, status } = {}) => http.get('/api/jobs', { datasetVersionId, status }),
  retry: (id) => http.post(`/api/jobs/${id}/retry`),
};
