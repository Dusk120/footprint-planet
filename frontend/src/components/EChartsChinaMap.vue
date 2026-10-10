<script setup>
import { ref, onMounted, onUnmounted, watch, shallowRef, computed } from 'vue'
import * as echarts from 'echarts/core'
// 按需注册：组件里实际用到的 series 类型是 custom（足迹标记、城市边界）、
// scatter（城市点）、effectScatter（高亮城市），缺一个就会在 setOption 时
// 报 "Series xxx is used but not imported" 并连带 getProgressive 崩溃。
import { CustomChart, ScatterChart, EffectScatterChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  VisualMapComponent,
  GeoComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  CustomChart,
  ScatterChart,
  EffectScatterChart,
  TitleComponent,
  TooltipComponent,
  VisualMapComponent,
  GeoComponent,
  CanvasRenderer
])
import { MUNI_POINTS } from '../data/cities'
import { useAppStore } from '../stores/app'
import { cityColorOf, withAlpha } from '../utils/cityColor'

const store = useAppStore()

const emit = defineEmits(['cityClick'])
const props = defineProps({
  cities: { type: Array, default: () => [] }
})

const chartEl = ref(null)
const chart = shallowRef(null)

let cityPoints = []            // 全部地级行政区（来自 china-cities.json）
let cityRegions = new Map()    // adcode -> { name, rings }，用于整市涂色（直辖市用省级完整轮廓）
let districtRings = new Map()  // 直辖市 adcode -> 各区县环，用于放大后的虚线边界
let regionByName = new Map()   // 地级市全名 -> adcode，用于匹配已点亮城市
let provinceByCode = new Map() // 省级 adcode -> 省份全名，用于点击时回填省份
let currentZoom = 1
let geoLoaded = false
let boundaryData = []          // 抽稀后的地级市边界（放大后绘制）
let showBoundaries = false     // 是否显示地级市边界（ECharts 5 的 series.show 对 custom 无效，用此标志控制）
let showCapitals = false       // 是否显示省会点（ECharts 5 的 series.show 对 scatter 不可靠，改用 data 空/满控制）
let showAllCities = false      // 是否显示所有地级市点
let capitalsCache = []         // 未点亮省会点数据缓存
let othersCache = []           // 未点亮地级市点数据缓存

// 中国范围（防止拖出到其他国家）
const ZOOM_CENTER = [104.5, 35.5]
const BOUNDS_SW = [73.5, 3.5]
const BOUNDS_NE = [135.2, 53.6]

// ECharts 缩放倍数是相对初始(1=全国)的倍数
const ZOOM_CAPITAL = 3       // 放大到该倍数显示省会名
const ZOOM_ALL_CITIES = 6    // 再放大显示所有地级行政区名 + 地级市边界
const ZOOM_DRAG_ENABLE = 1.5 // 放大到该倍数后才允许拖动

// 地级市边界抽稀步长（坐标点多，抽稀后大幅降低渲染开销）
const BOUNDARY_STRIDE = 5

const MUNI = new Set(['110000', '120000', '310000', '500000', '810000', '820000', '710000'])

const PROVINCE_SHORT = {
  '北京市': '北京', '天津市': '天津', '河北省': '河北', '山西省': '山西',
  '内蒙古自治区': '内蒙古', '辽宁省': '辽宁', '吉林省': '吉林', '黑龙江省': '黑龙江',
  '上海市': '上海', '江苏省': '江苏', '浙江省': '浙江', '安徽省': '安徽',
  '福建省': '福建', '江西省': '江西', '山东省': '山东', '河南省': '河南',
  '湖北省': '湖北', '湖南省': '湖南', '广东省': '广东', '广西壮族自治区': '广西',
  '海南省': '海南', '重庆市': '重庆', '四川省': '四川', '贵州省': '贵州',
  '云南省': '云南', '西藏自治区': '西藏', '陕西省': '陕西', '甘肃省': '甘肃',
  '青海省': '青海', '宁夏回族自治区': '宁夏', '新疆维吾尔自治区': '新疆',
  '台湾省': '台湾', '香港特别行政区': '香港', '澳门特别行政区': '澳门'
}

function getThemeColors() {
  const isDark = store.theme === 'dark'
  return isDark
    ? {
        area: '#141d3a', border: 'rgba(255,255,255,0.18)',
        emArea: '#182048', emBorder: '#64748b',
        label: 'rgba(255,255,255,0.45)',
        labelShadow: 'rgba(0,0,0,0.8)',
        cityLabel: 'rgba(255,255,255,0.8)',
        markerBorder: 'rgba(0,0,0,0.4)',
        markerNormal: 'rgba(255,255,255,0.85)',
        visitedShadow: 'rgba(56,189,248,0.6)',
        tooltipBg: 'rgba(8,8,8,.97)',
        tooltipBorder: 'rgba(71,85,105,.5)',
        tooltipText: '#f8fafc',
        scatterNormal: 'rgba(255,255,255,0.85)',
        scatterOther: 'rgba(255,255,255,0.55)',
        boundaryStroke: 'rgba(56,189,248,0.38)'
      }
    : {
        area: '#e2e8f0', border: 'rgba(0,0,0,0.12)',
        emArea: '#cbd5e1', emBorder: '#94a3b8',
        label: 'rgba(30,41,59,0.55)',
        labelShadow: 'rgba(255,255,255,0.6)',
        cityLabel: 'rgba(30,41,59,0.8)',
        markerBorder: 'rgba(255,255,255,0.6)',
        markerNormal: 'rgba(30,41,59,0.85)',
        visitedShadow: 'rgba(56,189,248,0.5)',
        tooltipBg: 'rgba(255,255,255,.97)',
        tooltipBorder: 'rgba(0,0,0,.12)',
        tooltipText: '#1e293b',
        scatterNormal: 'rgba(30,41,59,0.85)',
        scatterOther: 'rgba(30,41,59,0.5)',
        boundaryStroke: 'rgba(56,189,248,0.45)'
      }
}

// 已点亮城市：按 adcode / 名字 构建索引
const visitedCities = computed(() => props.cities.filter(c => c.adcode || c.name))
const visitedByAdcode = computed(() => {
  const m = new Map()
  props.cities.forEach(c => { if (c.adcode) m.set(String(c.adcode), c) })
  return m
})
const visitedNames = computed(() => new Set(props.cities.map(c => c.name)))

function provinceShort(name) {
  return PROVINCE_SHORT[name] || name
}

function extractRings(geometry) {
  const polys = geometry.type === 'MultiPolygon' ? geometry.coordinates : [geometry.coordinates]
  const rings = []
  polys.forEach(poly => { if (poly && poly[0]) rings.push(poly[0]) })
  return rings
}

// ---------- 数据加载 ----------
// /textures/*.json 都是标准 JSON，直接 res.json() 解析。
// 解析结果挂到 window 上与 src/data/cities.js 共用，避免重复请求。
async function fetchGeoJson(path) {
  const res = await fetch(path)
  if (!res.ok) throw new Error(`加载 ${path} 失败（HTTP ${res.status}）`)
  return await res.json()
}

async function loadGeo() {
  // 1) 省级边界
  if (!window.CHINA_GEO) {
    window.CHINA_GEO = await fetchGeoJson('/textures/china-geo.json')
  }
  const china = window.CHINA_GEO
  if (!china || !Array.isArray(china.features)) {
    throw new Error('省级边界数据格式异常（缺少 features）')
  }
  echarts.registerMap('china', china)

  // 2) 各省地级市边界（含直辖市区县）
  if (!window.CHINA_PROVINCES_GEO) {
    window.CHINA_PROVINCES_GEO = await fetchGeoJson('/textures/china-provinces-geo.json')
  }
  const provGeo = window.CHINA_PROVINCES_GEO
  if (!provGeo || typeof provGeo !== 'object' || Array.isArray(provGeo)) {
    throw new Error('各省边界数据格式异常（应为 adcode -> FeatureCollection 的对象）')
  }

  provinceByCode = new Map()
  china.features.forEach(f => provinceByCode.set(String(f.properties.adcode), f.properties.name))

  cityRegions = new Map()
  districtRings = new Map()
  regionByName = new Map()
  Object.entries(provGeo).forEach(([provCode, fc]) => {
    const feats = fc.features || []
    if (MUNI.has(provCode)) {
      // 涂色用省级完整轮廓，避免内部区县边界叠加产生线条
      const provFeat = china.features.find(f => String(f.properties.adcode) === provCode)
      const rings = provFeat ? extractRings(provFeat.geometry) : []
      cityRegions.set(provCode, { name: provinceByCode.get(provCode) || provCode, rings })
      // 放大后的虚线边界仍保留各区县环
      const dist = []
      feats.forEach(f => dist.push(...extractRings(f.geometry)))
      districtRings.set(provCode, dist)
    } else {
      feats.forEach(f => {
        const p = f.properties || {}
        const ad = String(p.adcode || '')
        if (!ad) return
        if (!cityRegions.has(ad)) cityRegions.set(ad, { name: p.name, rings: extractRings(f.geometry) })
        if (!regionByName.has(p.name)) regionByName.set(p.name, ad)
      })
    }
  })

  // 3) 全部地级行政区坐标（含自治州等）
  //    china-cities.json 把直辖市/特别行政区细分到区县（如万州区、中西区），台湾完全缺失，
  //    这里过滤区县级条目，并注入市级点（北京/上海/天津/重庆/香港/澳门/台湾）
  const cityRes = await fetch('/textures/china-cities.json')
  const citiesJson = await cityRes.json()
  cityPoints = []
  citiesJson.forEach(c => {
    const ad = regionByName.get(c.name) || ''
    if (!ad) return  // 过滤直辖市区县等无地级市匹配的条目
    cityPoints.push({
      name: c.name,
      value: [c.lng, c.lat],
      isCapital: !!c.isCapital,
      adcode: ad
    })
  })
  MUNI_POINTS.forEach(p => {
    cityPoints.push({
      name: p.name,
      value: [p.lng, p.lat],
      isCapital: true,
      adcode: p.adcode
    })
  })

  // 台湾无区县级数据，用省级轮廓支持整岛涂色
  if (!cityRegions.has('710000')) {
    const tw = china.features.find(f => String(f.properties.adcode) === '710000')
    if (tw) cityRegions.set('710000', { name: '台湾省', rings: extractRings(tw.geometry) })
  }

  // 4) 地级市边界抽稀（坐标点过多，按步长抽稀以降低渲染开销）
  boundaryData = []
  cityRegions.forEach((reg, key) => {
    const rings = districtRings.get(key) || reg.rings
    boundaryData.push({
      name: reg.name,
      rings: rings.map(ring => {
        if (ring.length <= BOUNDARY_STRIDE * 2) return ring
        const out = []
        for (let i = 0; i < ring.length; i += BOUNDARY_STRIDE) out.push(ring[i])
        if (out[out.length - 1] !== ring[0]) out.push(ring[0])
        return out
      })
    })
  })
}

// adcode -> 区域轮廓。直辖市的区县级编码（如北京 110100）回落到省级轮廓
function regionOf(adcode) {
  const ad = String(adcode || '')
  if (!ad) return null
  const direct = cityRegions.get(ad)
  if (direct) return direct
  const provCode = ad.slice(0, 2) + '0000'
  return MUNI.has(provCode) ? cityRegions.get(provCode) || null : null
}

// ---------- 已点亮城市整市涂色（始终显示） ----------
function makeVisitedRegionSeries() {
  const data = props.cities
    .filter(c => c.adcode)
    .map(c => {
      const reg = regionOf(c.adcode)
      return reg ? { name: c.name, rings: reg.rings, color: cityColorOf(c) } : null
    })
    .filter(Boolean)

  return {
    name: '已点亮区域',
    type: 'custom',
    coordinateSystem: 'geo',
    zlevel: 3,
    silent: true,
    renderItem(params, api) {
      // ECharts 5 的 custom series 无 params.data，需用闭包 + dataIndex 取数据项
      const item = data[params.dataIndex]
      if (!item) return
      const children = item.rings.map(ring => ({
        type: 'polygon',
        shape: { points: ring.map(p => api.coord(p)) },
        style: {
          fill: item.color,
          stroke: item.color,
          lineWidth: 2,
          fillOpacity: 0.5
        }
      }))
      return { type: 'group', children }
    },
    data
  }
}

// ---------- 地级市边界（放大后叠加显示） ----------
function makeCityBoundarySeries() {
  const tc = getThemeColors()
  return {
    name: '地级市边界',
    type: 'custom',
    coordinateSystem: 'geo',
    zlevel: 2,
    silent: true,
    renderItem(params, api) {
      if (!showBoundaries) return
      const item = boundaryData[params.dataIndex]
      if (!item) return
      const children = item.rings.map(ring => ({
        type: 'polygon',
        shape: { points: ring.map(p => api.coord(p)) },
        style: { fill: 'rgba(0,0,0,0)', stroke: tc.boundaryStroke, lineWidth: 1, lineDash: [4, 4] }
      }))
      return { type: 'group', children }
    },
    data: boundaryData
  }
}

// ---------- 城市散点（省会逐级显示） ----------
function makeScatterSeries() {
  const tc = getThemeColors()
  const visitedAd = visitedByAdcode.value
  const vNames = visitedNames.value

  const isVisited = (p) => (p.adcode && visitedAd.has(p.adcode)) || vNames.has(p.name)

  const toPoint = (p) => {
    const vc = p.adcode ? visitedAd.get(p.adcode) : null
    const visited = !!vc || vNames.has(p.name)
    return {
      name: vc ? vc.name : p.name,
      value: p.value,
      visited,
      color: vc?.color
    }
  }

  const baseLabel = {
    show: false,
    position: 'right',
    distance: 4,
    formatter: '{b}',
    fontWeight: (params) => params.data.visited ? 'bold' : 'normal',
    color: (params) => params.data.visited ? params.data.color : tc.cityLabel,
    textShadowColor: tc.labelShadow,
    textShadowBlur: 6
  }

  const capitals = cityPoints.filter(p => p.isCapital && !isVisited(p)).map(toPoint)
  const others = cityPoints.filter(p => !p.isCapital && !isVisited(p)).map(toPoint)
  capitalsCache = capitals
  othersCache = others

  return {
    capitals: {
      name: '省会',
      type: 'scatter',
      coordinateSystem: 'geo',
      symbol: 'circle',
      symbolSize: (v, params) => params.data.visited ? 12 : 8,
      itemStyle: { color: (params) => params.data.visited ? params.data.color : tc.scatterNormal },
      label: { ...baseLabel, fontSize: 11, show: showCapitals },
      labelLayout: { hideOverlap: true },
      show: false,
      data: showCapitals ? capitals : []
    },
    all: {
      name: '所有地级行政区',
      type: 'scatter',
      coordinateSystem: 'geo',
      symbol: 'circle',
      symbolSize: (v, params) => params.data.visited ? 12 : 6,
      itemStyle: { color: (params) => params.data.visited ? params.data.color : tc.scatterOther },
      label: { ...baseLabel, fontSize: 9.5, color: (params) => params.data.visited ? params.data.color : tc.scatterOther, show: showAllCities },
      labelLayout: { hideOverlap: true },
      show: false,
      data: showAllCities ? others : []
    }
  }
}

// ---------- 已点亮城市标记（涟漪，始终显示） ----------
function makeVisitedMarkerSeries() {
  return {
    name: '已点亮城市',
    type: 'effectScatter',
    coordinateSystem: 'geo',
    symbol: 'circle',
    // 这个 series 的数据全是已点亮城市，原来的 visited ? 12 : 9 恒取 12。
    // 12px 的核心 + 每边 8px 的 shadowBlur，视觉直径接近 28px，在小缩放级别下糊成一团。
    // 核心收到 8、光晕收到 5，整体小一圈；涟漪反而相对放大一点保住存在感
    symbolSize: 8,
    rippleEffect: { brushType: 'stroke', scale: 2.0, period: 3.5 },
    itemStyle: {
      color: (p) => p.data.color || '#38bdf8',
      shadowBlur: 5,
      shadowColor: (p) => withAlpha(p.data.color || '#38bdf8', 0.55)
    },
    label: {
      show: false,
      position: 'right',
      distance: 4,
      formatter: '{b}',
      fontWeight: 'bold',
      color: '#a5b4fc',
      fontSize: 12,
      textShadowColor: 'rgba(0,0,0,0.8)',
      textShadowBlur: 6
    },
    zlevel: 4,
    data: props.cities
      .filter(c => c.lng != null && c.lat != null)
      .map(c => ({ name: c.name, value: [c.lng, c.lat], visited: true, color: cityColorOf(c) }))
  }
}

function buildOption() {
  const tc = getThemeColors()
  const visitedRegion = makeVisitedRegionSeries()
  const visitedMarker = makeVisitedMarkerSeries()
  const cityBoundary = makeCityBoundarySeries()
  const scatter = makeScatterSeries()

  return {
    backgroundColor: 'transparent',
    animation: false,
    tooltip: {
      trigger: 'item',
      backgroundColor: tc.tooltipBg,
      borderColor: tc.tooltipBorder,
      borderWidth: 1,
      textStyle: { color: tc.tooltipText, fontSize: 12 },
      formatter: (p) => (p.data ? `<div style="font-weight:700;font-size:14px">${p.data.name}</div>` : '')
    },
    geo: {
      map: 'china',
      roam: 'scale',
      center: [...ZOOM_CENTER],
      zoom: 1.5,
      scaleLimit: { min: 1, max: 12 },
      itemStyle: {
        areaColor: tc.area,
        borderColor: tc.border,
        borderWidth: 1
      },
      label: {
        show: true,
        formatter: (p) => provinceShort(p.name),
        color: tc.label,
        fontSize: 12,
        fontWeight: 500,
        textShadowColor: tc.labelShadow,
        textShadowBlur: 4
      },
      emphasis: { label: { show: false }, itemStyle: { areaColor: tc.emArea, borderColor: tc.emBorder } }
    },
    series: [visitedRegion, visitedMarker, cityBoundary, scatter.capitals, scatter.all]
  }
}

function updateLayersByZoom(z) {
  if (!chart.value || !geoLoaded) return
  showBoundaries = z >= ZOOM_ALL_CITIES
  showCapitals = z >= ZOOM_CAPITAL
  showAllCities = z >= ZOOM_ALL_CITIES
  chart.value.setOption({
    geo: {
      label: { show: z < ZOOM_CAPITAL },
      roam: z >= ZOOM_DRAG_ENABLE ? true : 'scale'
    },
    series: [
      {},
      { label: { show: z >= ZOOM_CAPITAL } },
      {},
      { show: showCapitals, label: { show: showCapitals }, data: showCapitals ? capitalsCache : [] },
      { show: showAllCities, label: { show: showAllCities }, data: showAllCities ? othersCache : [] }
    ]
  })
}

function clampToChina(center) {
  if (!center) return
  const lng = Math.min(Math.max(center[0], BOUNDS_SW[0]), BOUNDS_NE[0])
  const lat = Math.min(Math.max(center[1], BOUNDS_SW[1]), BOUNDS_NE[1])
  if (Math.abs(lng - center[0]) > 0.01 || Math.abs(lat - center[1]) > 0.01) {
    chart.value.setOption({ geo: { center: [lng, lat] } })
  }
}

function handleGeoroam(params) {
  if (!geoLoaded) return
  // georoam 的 params.zoom 是相对缩放增量（如 1.1），
  // 必须从 getOption() 读取真实的绝对 zoom 才能做阈值判断
  const opt = chart.value.getOption()
  currentZoom = opt.geo?.[0]?.zoom || currentZoom
  updateLayersByZoom(currentZoom)
  clampToChina(params.center)
}

function handleClick(params) {
  if (!geoLoaded) return
  // 只响应城市散点/涟漪点，忽略 geo 省份区域、边界、涂色层
  if (params.seriesType !== 'scatter' && params.seriesType !== 'effectScatter') return
  const data = params.data
  if (!data || !data.name) return

  const visited = !!props.cities.find(c => c.name === data.name)
  let city = visited
    ? props.cities.find(c => c.name === data.name)
    : null

  if (!city) {
    const pt = cityPoints.find(p => p.name === data.name)
    if (!pt) return
    const ad = String(pt.adcode || '')
    city = {
      name: pt.name,
      lng: pt.value[0],
      lat: pt.value[1],
      adcode: pt.adcode,
      province: ad ? (provinceByCode.get(ad.slice(0, 2) + '0000') || '') : ''
    }
  }

  if (visited) {
    currentZoom = Math.max(currentZoom, 5)
    chart.value.setOption({ geo: { center: [city.lng, city.lat], zoom: currentZoom } })
    updateLayersByZoom(currentZoom)
  }
  emit('cityClick', { ...city, visited })
}

onMounted(() => {
  // init 必须放在同步阶段执行，此时 DOM 一定有效；
  // 若在 await 数据加载后才 init，期间组件被卸载会导致 chartEl 为空
  if (!chartEl.value) return
  chart.value = echarts.init(chartEl.value, { renderer: 'canvas' })

  chart.value.on('georoam', handleGeoroam)
  chart.value.on('click', handleClick)

  const ro = new ResizeObserver(() => chart.value?.resize())
  ro.observe(chartEl.value)
  chart.value._ro = ro

  loadGeo()
    .then(() => {
      if (!chart.value) return // 组件已卸载
      geoLoaded = true
      chart.value.setOption(buildOption(), true)
      currentZoom = 1.5
      updateLayersByZoom(1.5)
    })
    .catch(e => console.error('ECharts 地图加载失败:', e))
})

// 整体重建（城市变了 / 换主题时用）。
// buildOption() 里的 zoom 和 center 是初始值，直接重建会把视图缩回初始级别；
// 而标注层级标志（showCapitals / showAllCities）是按「真实缩放」算的 ——
// 两者不一致就会出现「地图缩得很小、却糊满全部城市名」的错位。
// 所以重建前先记下当前视图，重建后一并恢复。
function rebuildKeepingView() {
  const geo = chart.value.getOption()?.geo?.[0] || {}
  const zoom = geo.zoom || currentZoom
  const center = geo.center
  chart.value.setOption(buildOption(), true)
  chart.value.setOption({ geo: { zoom, ...(center ? { center: [...center] } : {}) } })
  currentZoom = zoom
  updateLayersByZoom(zoom)
}

watch(() => props.cities, () => {
  if (!chart.value || !geoLoaded) return
  rebuildKeepingView()
}, { deep: true })

watch(() => store.theme, () => {
  if (!chart.value || !geoLoaded) return
  rebuildKeepingView()
})

onUnmounted(() => {
  chart.value?._ro?.disconnect()
  chart.value?.dispose()
  chart.value = null
})

function handleZoomIn() {
  if (!chart.value || !geoLoaded) return
  const newZoom = Math.min(currentZoom * 1.5, 12)
  currentZoom = newZoom
  chart.value.setOption({ geo: { zoom: newZoom } })
  updateLayersByZoom(newZoom)
}

function handleZoomOut() {
  if (!chart.value || !geoLoaded) return
  const newZoom = Math.max(currentZoom / 1.5, 1)
  currentZoom = newZoom
  chart.value.setOption({ geo: { zoom: newZoom } })
  updateLayersByZoom(newZoom)
}

defineExpose({ chart, handleZoomIn, handleZoomOut })
</script>

<template>
  <div ref="chartEl" class="echarts-map"></div>
</template>

<style scoped>
.echarts-map {
  width: 100%;
  height: 100%;
}
</style>
