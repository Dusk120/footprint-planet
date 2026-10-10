import { http } from './http'

export const noteApi = {
  list: (params) => http.get('/notes', params),
  create: (data) => http.post('/notes', data),
  remove: (id) => http.delete(`/notes/${id}`),
  like: (id, like) => http.post(`/notes/${id}/like`, { like }),
  getComments: (noteId, params) => http.get(`/notes/${noteId}/comments`, params),
  createComment: (noteId, data) => http.post(`/notes/${noteId}/comments`, data),
  removeComment: (noteId, commentId) => http.delete(`/notes/${noteId}/comments/${commentId}`),
  // 评论与回复共用（回复也是评论）
  likeComment: (noteId, commentId, like) =>
    http.post(`/notes/${noteId}/comments/${commentId}/like`, { like })
}
