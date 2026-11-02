import { http } from './http'

function pageQuery({ page, pageSize } = {}) {
  const params = {}
  if (page) params.page = page
  if (pageSize) params.pageSize = pageSize
  return params
}

export const planApi = {
  // ===== 基本信息 =====
  // 我的规划：后端真分页，返回 { list, total, page, pageSize }（列表只含基本字段，不含 items/legs）
  list: (query) => http.get('/plans', pageQuery(query)),
  // 规划详情：返回 PlanDetailVO（含 items/legs/collaborators/myRole/version 等）
  detail: (id) => http.get(`/plans/${id}`),
  create: (data) => http.post('/plans', data),
  update: (id, data) => http.put(`/plans/${id}`, data),
  remove: (id) => http.delete(`/plans/${id}`),
  // 复制他人公开规划到自己的规划，只传源 id
  copy: (id) => http.post(`/plans/${id}/copy`),
  // 设置封面（仅 owner），body { image }
  setCover: (id, image) => http.post(`/plans/${id}/cover`, { image }),

  // ===== 行程条目 =====
  // 添加条目（editor+）
  addItem: (planId, data) => http.post(`/plans/${planId}/items`, data),
  // 更新条目（editor+）
  updateItem: (planId, itemId, data) => http.put(`/plans/${planId}/items/${itemId}`, data),
  // 删除条目（editor+）
  deleteItem: (planId, itemId) => http.delete(`/plans/${planId}/items/${itemId}`),
  // 批量排序（editor+）：days = [{ dayNo, itemIds }]，version 乐观锁
  reorderItems: (planId, days, version) => http.put(`/plans/${planId}/items/reorder`, { days, version }),

  // ===== 交通段 =====
  // 保存交通段（editor+），body 含 polyline（真实路线坐标串，可空）
  saveLeg: (planId, data) => http.post(`/plans/${planId}/legs`, data),
  // 后端算路（高德 Web 服务）：from/to 坐标，返回多交通方式方案 + polyline
  routeSearch: (from, to) => http.post('/route/search', {
    fromLng: from.lng, fromLat: from.lat, toLng: to.lng, toLat: to.lat
  }),

  // ===== 协作控制（owner）=====
  // 锁定 / 解锁，body { locked }
  setLock: (id, locked) => http.put(`/plans/${id}/lock`, { locked }),
  // 设置 / 取消创作截止时间，body { deadline }（null 表示取消）
  setDeadline: (id, deadline) => http.put(`/plans/${id}/deadline`, { deadline }),
  // 移除协作者成员（仅 owner）
  removeCollaborator: (planId, userId) => http.delete(`/plans/${planId}/collaborators/${userId}`),

  // ===== 邀请链接 =====
  // 签发邀请（仅 owner），body { role, expiresAt, maxUses }
  createInvite: (planId, data) => http.post(`/plans/${planId}/invites`, data),
  // 核销邀请（登录用户接受链接）
  acceptInvite: (token) => http.post(`/invites/${token}/accept`),
  // 更新邀请（仅 owner）：改角色/次数/过期时间，token 不变、链接不变
  updateInvite: (planId, inviteId, data) => http.put(`/plans/${planId}/invites/${inviteId}`, data),
  // 撤销邀请（仅 owner）
  revokeInvite: (planId, inviteId) => http.delete(`/plans/${planId}/invites/${inviteId}`),
  // 邀请链接列表（仅 owner）
  listInvites: (planId) => http.get(`/plans/${planId}/invites`)
}
