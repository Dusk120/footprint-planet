import { http } from './http'

// 消息通知 REST 接口（与后端 NotificationController 一一对应）
export const notificationApi = {
  // 游标分页拉某类通知：type 取值 'like' | 'comment'，cursor 为上一页游标（首页省略/传 null）
  // 响应：{ list, nextCursor, hasMore, unreadCount, totalUnreadCount }
  list: (params) => http.get('/notifications', params),
  // 标记单条已读
  read: (id) => http.put(`/notifications/${id}/read`),
  // 标记某类通知全部已读（仅当前 tab）：type 取值 'like' | 'comment'
  readAll: (type) => http.post('/notifications/read-all', null, { type }),
}
