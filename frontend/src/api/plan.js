import { http } from './http'

function pageQuery({ page, pageSize } = {}) {
  const params = {}
  if (page) params.page = page
  if (pageSize) params.pageSize = pageSize
  return params
}

export const planApi = {
  // 我的规划：后端真分页，返回 { list, total, page, pageSize }
  list: (query) => http.get('/plans', pageQuery(query)),
  detail: (id) => http.get(`/plans/${id}`),
  create: (data) => http.post('/plans', data),
  update: (id, data) => http.put(`/plans/${id}`, data),
  remove: (id) => http.delete(`/plans/${id}`),
  // 复制他人公开规划到自己的规划（副本自动私有），只传源 id
  copy: (id) => http.post(`/plans/${id}/copy`)
}
