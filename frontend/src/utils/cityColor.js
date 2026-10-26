// 城市主题色：12 等份色相（HSL 均分、统一明度），保证地图上相邻城市可区分。
// 原色板里 #38bdf8 / #60a5fa / #3b82f6 三个蓝过于接近，整市涂色时容易串色。
export const CITY_COLORS = [
  '#dd3c3c', '#dd8c3c', '#dddd3c', '#8cdd3c',
  '#3cdd3c', '#3cdd8c', '#3cdddd', '#3c8cdd',
  '#3c3cdd', '#8c3cdd', '#dd3cdd', '#dd3c8c'
]

// 按城市名稳定分配一个默认色：同一城市每次都得同一色
export function pickCityColor(name) {
  const s = name || ''
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0
  return CITY_COLORS[h % CITY_COLORS.length]
}

// 城市对象 → 显示用色（库里存了就用存的，否则按名字算）
export function cityColorOf(city) {
  return city?.color || pickCityColor(city?.name)
}

// 主色 '#38bdf8' + 0.6 → 'rgba(56,189,248,0.6)'，给 ECharts shadowColor 兜底用
export function withAlpha(hex, a) {
  const m = /^#([0-9a-f]{6})$/i.exec(hex || '')
  if (!m) return `rgba(56,189,248,${a})`
  const n = parseInt(m[1], 16)
  return `rgba(${(n >> 16) & 255},${(n >> 8) & 255},${n & 255},${a})`
}
