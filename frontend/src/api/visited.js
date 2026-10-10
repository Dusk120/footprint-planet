import { http } from './http'

export const visitedApi = {
  list: () => http.get('/visited-cities'),
  // 旅行回忆：展平列表（后端按游玩时间倒序）；keyword 可选，由后端对城市名/省份模糊匹配
  memories: (params) => http.get('/visited-cities/memories', params),
  add: (data) => http.post('/visited-cities', data),
  update: (id, data) => http.put(`/visited-cities/${id}`, data),
  remove: (id) => http.delete(`/visited-cities/${id}`)
}
