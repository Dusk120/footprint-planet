import { http } from './http'

// 管理端接口（路径对应后端 /api/admin/**，http 层会自动加 /api 前缀）
export const adminApi = {
  // 仪表盘概览：{userCount,noteCount,feedbackCount,pendingFeedbackCount,trend,hotCities,hotNotes}
  getOverview: () => http.get('/admin/dashboard/overview'),
  // 用户管理
  getUsers: (params) => http.get('/admin/users', params),          // {keyword,page,pageSize}
  getUser: (id) => http.get(`/admin/users/${id}`),                 // 详情（预留）
  setUserStatus: (id, status) => http.put(`/admin/users/${id}/status`, { status }), // status: 0|1
  resetUserPassword: (id, password) => http.put(`/admin/users/${id}/password`, { password }),
  // 笔记管理
  getNotes: (params) => http.get('/admin/notes', params),          // {keyword,page,pageSize}
  removeNote: (id) => http.delete(`/admin/notes/${id}`),
  // 反馈管理
  getFeedbacks: (params) => http.get('/admin/feedbacks', params),  // {handled:0|1(可省),page,pageSize}
  setFeedbackHandled: (id, handled) => http.put(`/admin/feedbacks/${id}/handled`, { handled }), // handled: boolean
  removeFeedback: (id) => http.delete(`/admin/feedbacks/${id}`)
}
