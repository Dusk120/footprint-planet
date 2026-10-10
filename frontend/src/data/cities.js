// 全国可点亮城市数据（与地图共用，保证“选择城市”和地图点一致）
// china-cities.json 把直辖市/特别行政区细分到区县（万州区、中西区…），台湾缺失，
// 这里过滤区县级条目并注入市级点（北京/上海/天津/重庆/香港/澳门/台湾）

export const MUNI_POINTS = [
  { name: '北京', lng: 116.41, lat: 39.90, adcode: '110000' },
  { name: '上海', lng: 121.47, lat: 31.23, adcode: '310000' },
  { name: '天津', lng: 117.19, lat: 39.13, adcode: '120000' },
  { name: '重庆', lng: 106.55, lat: 29.56, adcode: '500000' },
  { name: '香港', lng: 114.17, lat: 22.32, adcode: '810000' },
  { name: '澳门', lng: 113.55, lat: 22.19, adcode: '820000' },
  { name: '台湾', lng: 121.51, lat: 25.04, adcode: '710000' }
]

let cache = null

// \u52A0\u8F7D GeoJSON \u6570\u636E\uFF1A\u7ED3\u679C\u6302\u5230 window \u4E0A\u4E0E EChartsChinaMap.vue \u5171\u7528\uFF0C\u907F\u514D\u91CD\u590D\u8BF7\u6C42
async function loadGeoJson(path, cacheKey) {
  if (cacheKey && window[cacheKey]) return window[cacheKey]
  const res = await fetch(path)
  if (!res.ok) throw new Error(`\u52A0\u8F7D ${path} \u5931\u8D25\uFF08HTTP ${res.status}\uFF09`)
  const json = await res.json()
  if (cacheKey) window[cacheKey] = json
  return json
}

// 返回 [{ name, lng, lat, adcode, province, isCapital }]，带模块级缓存
export async function loadCityPoints() {
  if (cache) return cache
  const muniSet = new Set(MUNI_POINTS.map(p => p.adcode))

  const provGeo = await loadGeoJson('/textures/china-provinces-geo.json', 'CHINA_PROVINCES_GEO')
  const china = await loadGeoJson('/textures/china-geo.json', 'CHINA_GEO')

  const regionByName = new Map()
  Object.entries(provGeo).forEach(([provCode, fc]) => {
    if (muniSet.has(provCode)) return
    ;(fc.features || []).forEach(f => {
      const name = (f.properties || {}).name
      const ad = String((f.properties || {}).adcode || '')
      if (name && ad && !regionByName.has(name)) regionByName.set(name, ad)
    })
  })
  const provinceByCode = new Map()
  china.features.forEach(f => provinceByCode.set(String(f.properties.adcode), f.properties.name))

  const res = await fetch('/textures/china-cities.json')
  const cities = await res.json()

  const points = []
  cities.forEach(c => {
    const ad = regionByName.get(c.name) || ''
    if (!ad) return
    points.push({
      name: c.name,
      lng: c.lng,
      lat: c.lat,
      isCapital: !!c.isCapital,
      adcode: ad,
      province: provinceByCode.get(ad.slice(0, 2) + '0000') || ''
    })
  })
  MUNI_POINTS.forEach(p => {
    points.push({
      ...p,
      isCapital: true,
      province: provinceByCode.get(p.adcode) || ''
    })
  })

  cache = points
  return cache
}
