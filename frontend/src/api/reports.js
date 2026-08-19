import { http } from './client.js';

export const reportsApi = {
  list: (datasetId) => http.get('/api/reports', { datasetId }),
  create: ({ datasetVersionId, reportType }) => http.post('/api/reports', { datasetVersionId, reportType }),
  get: (id) => http.get(`/api/reports/${id}`),
  kpiOverview: () => http.get('/api/reports/kpis/overview'),
};
