import { http } from './http'

export const userApi = {
  updateMe: (data) => http.put('/users/me', data),
  // 作者主页：公开资料 + 统计（按 userId）
  getProfile: (id) => http.get(`/users/${id}`),
  // 作者公开规划（按 userId）：后端真分页，返回 { list, total, page, pageSize }
  getPublicPlans: (id, query) => {
    const params = {}
    if (query?.page) params.page = query.page
    if (query?.pageSize) params.pageSize = query.pageSize
    return http.get(`/users/${id}/plans`, params)
  }
}
