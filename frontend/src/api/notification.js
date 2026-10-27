import { http } from './http'

// 消息通知 REST 接口（与后端 NotificationController 一一对应）
export const notificationApi = {
  // 分页拉通知列表（响应含 unreadCount 未读总数）
  list: (params) => http.get('/notifications', params),
  // 标记单条已读
  read: (id) => http.put(`/notifications/${id}/read`),
  // 全部标记已读
  readAll: () => http.post('/notifications/read-all'),
}
