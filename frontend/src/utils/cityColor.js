// 城市主题色：与 visited_city.color 里已有的取值保持一致
export const CITY_COLORS = [
  '#38bdf8', '#22c55e', '#f59e0b', '#a855f7',
  '#fb7185', '#06b6d4', '#f97316', '#34d399',
  '#60a5fa', '#f472b6', '#a78bfa', '#3b82f6'
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

// '#38bdf8' + 0.6 → 'rgba(56,189,248,0.6)'，给 ECharts shadowColor 用
export function withAlpha(hex, a) {
  const m = /^#([0-9a-f]{6})$/i.exec(hex || '')
  if (!m) return `rgba(56,189,248,${a})`
  const n = parseInt(m[1], 16)
  return `rgba(${(n >> 16) & 255},${(n >> 8) & 255},${n & 255},${a})`
}
