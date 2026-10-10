// 后端 LocalDateTime 默认序列化成 ISO 形式「2026-07-03T14:20:00」，
// 页面上统一显示为空格分隔的「2026-07-03 14:20:00」。
export function formatDateTime(v) {
  return v ? String(v).replace('T', ' ') : ''
}

// 点赞数 / 评论数统一格式：1000 以上折算成「2.4k」。
// 原先这个函数只写在 TravelNoteCard 里，现在社区卡片、我的、作者主页三处共用一份，
// 免得各写各的、慢慢跑偏（这个项目已经吃过「同一功能两份实现」的亏）
export function formatLikes(n) {
  const v = Number(n) || 0
  return v >= 1000 ? (v / 1000).toFixed(1) + 'k' : v
}
