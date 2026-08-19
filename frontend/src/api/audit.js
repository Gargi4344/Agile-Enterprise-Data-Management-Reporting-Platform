import { http } from './client.js';

export const auditApi = {
  list: ({ targetType, targetId, actorId, page = 0, size = 20 } = {}) =>
    http.get('/api/audit', { targetType, targetId, actorId, page, size }),
};
