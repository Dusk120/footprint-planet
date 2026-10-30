<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'

// 高德 JSAPI key（前端地图展示/选点）
const AMAP_KEY = 'a1fea35eb18262ade3ed6357ae119597'
// 安全密钥（securityJsCode）：申请 key 时勾选「安全密钥」则必填，否则 JSAPI 2.0 报 INVALID_USER_SCODE
const AMAP_SECURITY = '632b70f33258fe8b573e880da2dd0dec'

const props = defineProps({
  // 行程条目：{ id, name, lng, lat, dayNo, sortNo }
  items: { type: Array, default: () => [] },
  // 是否可点击地图选点（编辑者且未锁定时为 true）
  editable: { type: Boolean, default: false },
  // 目的地名称（用于地理编码定位地图初始中心，可为空）
  destination: { type: String, default: '' }
})
const emit = defineEmits(['select'])

const mapEl = ref(null)
const loading = ref(true)

let AMap = null
let map = null
let geocoder = null
let placeSearch = null
let markers = []
let polylines = []
let focusMarker = null   // 搜索结果临时定位标记
let lastPointCount = -1  // 上次有效地点数量，用于判断「新增地点」才自适应视野

// 是否显示标记点名称标签（可切换隐藏，避免地点密集时名称互相遮挡）；本地记忆
const SHOW_LABELS_KEY = 'footprint-planet:map-show-labels'
const showLabels = ref(localStorage.getItem(SHOW_LABELS_KEY) !== '0')
function toggleLabels() {
  showLabels.value = !showLabels.value
  localStorage.setItem(SHOW_LABELS_KEY, showLabels.value ? '1' : '0')
  render()
}

// 不同天的路线用不同颜色区分（循环取色）
const DAY_COLORS = ['#38bdf8', '#f97316', '#22c55e', '#f43f5e', '#a855f7', '#eab308']

async function init() {
  try {
    // 安全密钥必须先于 load 注入 window._AMapSecurityConfig，
    // 否则地图瓦片能加载，但 Geocoder/PlaceSearch 等 Web 服务类插件会静默失败（INVALID_USER_SCODE）
    if (AMAP_SECURITY) {
      window._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY }
    }
    AMap = await AMapLoader.load({
      key: AMAP_KEY,
      version: '2.0',
      ...(AMAP_SECURITY ? { securityJsCode: AMAP_SECURITY } : {}),
      plugins: ['AMap.Geocoder', 'AMap.PlaceSearch', 'AMap.Driving', 'AMap.Walking', 'AMap.Riding', 'AMap.Transfer']
    })
    map = new AMap.Map(mapEl.value, { zoom: 11, center: [116.397428, 39.90923] })
    geocoder = new AMap.Geocoder({})
    placeSearch = new AMap.PlaceSearch({ pageSize: 10 })
    // 根据目的地定位地图初始中心（地理编码：地址 → 坐标）
    if (props.destination) {
      geocoder.getLocation(props.destination, (status, result) => {
        if (status === 'complete' && result.geocodes && result.geocodes.length) {
          const loc = result.geocodes[0].location
          map.setZoomAndCenter(11, [loc.lng, loc.lat])
        }
      })
    }
    if (props.editable) {
      map.on('click', handleMapClick)
    }
    loading.value = false
    render()
  } catch (e) {
    loading.value = false
    console.error('高德地图加载失败', e)
  }
}

function handleMapClick(e) {
  // 点击地图选点：逆地理编码拿地点名 + POI id，回传给父组件
  geocoder.getAddress([e.lnglat.lng, e.lnglat.lat], (status, result) => {
    if (status !== 'complete' || !result || result.info !== 'OK') {
      console.warn('[AmapMap] 逆地理编码失败', status, result)
      emit('select', { lng: e.lnglat.lng, lat: e.lnglat.lat, name: '', poiId: '' })
      return
    }
    const name = extractName(result)
    const poiId = result.regeocode?.pois?.[0]?.id || ''
    emit('select', { lng: e.lnglat.lng, lat: e.lnglat.lat, name, poiId })
  })
}

// 从逆地理编码结果提取简洁地点名：优先最近 POI 名，否则去掉省市区前缀
function extractName(result) {
  const regeocode = result.regeocode
  if (!regeocode) return ''
  const pois = regeocode.pois
  if (pois && pois.length && pois[0]?.name) return pois[0].name
  const ac = regeocode.addressComponent || {}
  let name = regeocode.formattedAddress || ''
  const prefix = [ac.province, ac.city, ac.district, ac.township].filter(Boolean).join('')
  if (prefix && name.startsWith(prefix)) name = name.slice(prefix.length)
  if (name.length > 15) name = name.slice(0, 15)
  return name
}

// 地点搜索（POI）：供父组件「添加地点」搜索真实地点，返回 { name, address, lng, lat, poiId }
function search(keyword) {
  return new Promise((resolve) => {
    if (!placeSearch || !keyword) { resolve([]); return }
    placeSearch.search(keyword, (status, result) => {
      if (status === 'complete' && result && result.info === 'OK' && result.poiList && result.poiList.pois) {
        resolve(result.poiList.pois.map(p => ({
          name: p.name,
          address: p.address || '',
          lng: p.location.lng,
          lat: p.location.lat,
          poiId: p.id
        })))
      } else {
        console.warn('[AmapMap] 地点搜索失败', status, result)
        resolve([])
      }
    })
  })
}

// 出行方案计算：from/to 为 {lng, lat}，返回 [{ mode, label, duration, distance, summary }]
// duration 单位分钟、distance 单位公里（展示用；保存 plan_leg 时前端换算成秒/米）
// 真实调用高德 JSAPI 路线规划插件（Walking/Driving/Riding/Transfer），非 mock
function routeSearch(from, to) {
  return new Promise((resolve) => {
    if (!AMap || !from?.lng || !to?.lng) { resolve([]); return }
    const modes = [
      { mode: 'walking', label: '步行', Cls: AMap.Walking },
      { mode: 'driving', label: '驾车', Cls: AMap.Driving },
      { mode: 'riding', label: '骑行', Cls: AMap.Riding },
      { mode: 'transit', label: '公交', Cls: AMap.Transfer, isTransit: true }
    ]
    const results = []
    let pending = modes.length
    const checkDone = () => { if (--pending <= 0) resolve(results) }

    const startModes = (city) => {
      modes.forEach(({ mode, label, Cls, isTransit }) => {
        try {
          const opts = isTransit
            ? { city: city || '', policy: AMap.TransferPolicy.LEAST_TIME }
            : {}
          const plugin = new Cls(opts)
          plugin.search([from.lng, from.lat], [to.lng, to.lat], (status, result) => {
            if (status === 'complete') {
              // 公交返回 result.plans，其余返回 result.routes
              const list = isTransit ? result.plans : result.routes
              if (list && list.length) {
                const r = list[0]
                const minutes = Math.max(1, Math.round((r.time || 0) / 60))
                const km = Math.round((r.distance || 0) / 100) / 10
                results.push({
                  mode,
                  label,
                  duration: minutes,
                  distance: km,
                  summary: `${label}约 ${minutes} 分钟 · ${km} 公里`
                })
              }
            }
            checkDone()
          })
        } catch (e) {
          console.warn(`[AmapMap] ${mode} 路线规划失败`, e)
          checkDone()
        }
      })
    }

    // 公交规划需要城市名，先逆地理编码取起点所在城市
    geocoder.getAddress([from.lng, from.lat], (status, result) => {
      const city = (status === 'complete' && result?.regeocode)
        ? (result.regeocode.addressComponent?.city || '')
        : ''
      startModes(city)
    })
  })
}

// 定位显示某个搜索到的地点（地图中心移到该点 + 临时标记），不加入计划
function focusOn({ lng, lat, name }) {
  if (!map || !AMap || lng == null || lat == null) return
  if (focusMarker) { focusMarker.setMap(null); focusMarker = null }
  focusMarker = new AMap.Marker({
    position: [lng, lat],
    label: {
      content: `<div style="background:var(--accent-primary, #38bdf8);color:#fff;padding:3px 10px;border-radius:12px;font-size:12px;white-space:nowrap;box-shadow:0 2px 8px rgba(0,0,0,.3)">${name || '地点'}</div>`,
      offset: new AMap.Pixel(0, -32)
    }
  })
  focusMarker.setMap(map)
  map.setZoomAndCenter(14, [lng, lat])
}

defineExpose({ search, routeSearch, focusOn })

function render() {
  if (!map || !AMap) return
  markers.forEach(m => m.remove())
  markers = []
  polylines.forEach(p => p.setMap(null))
  polylines = []

  const pts = props.items.filter(i => i.lng != null && i.lat != null)
  if (!pts.length) return

  // 标记点 + 地点名标签
  markers = pts.map(p => {
    const m = new AMap.Marker({
      position: [p.lng, p.lat],
      label: showLabels.value ? {
        content: `<div style="background:rgba(0,0,0,.55);color:#fff;padding:2px 8px;border-radius:10px;font-size:12px;white-space:nowrap">${p.name || ''}</div>`,
        offset: new AMap.Pixel(0, -28)
      } : undefined
    })
    m.setMap(map)
    return m
  })

  // 连线：按 dayNo 分组，每天一条不同颜色的线
  const byDay = {}
  pts.forEach(p => {
    const d = p.dayNo ?? 0
    ;(byDay[d] = byDay[d] || []).push(p)
  })
  const dayNumbers = Object.keys(byDay).map(Number).sort((a, b) => a - b)
  dayNumbers.forEach((dayNo, idx) => {
    const list = (byDay[dayNo] || []).sort((a, b) => (a.sortNo ?? 0) - (b.sortNo ?? 0))
    if (list.length < 2) return
    const path = list.map(p => [p.lng, p.lat])
    const pl = new AMap.Polyline({
      path,
      strokeColor: DAY_COLORS[idx % DAY_COLORS.length],
      strokeWeight: 3,
      strokeDasharray: [8, 6],
      lineJoin: 'round',
      showDir: true
    })
    pl.setMap(map)
    polylines.push(pl)
  })

  // 自适应视野：只在「新增地点」（有效点数量增加）时调用，避免编辑/删除/排序每次操作都跳视野 + 重载瓦片
  const overlays = [...markers, ...polylines]
  const shouldFit = pts.length > lastPointCount
  lastPointCount = pts.length
  if (overlays.length && shouldFit) map.setFitView(overlays, false, [60, 60, 60, 60])
}

watch(() => props.items, render, { deep: true })

onMounted(init)
onUnmounted(() => {
  map?.destroy()
})
</script>

<template>
  <div class="amap-wrap">
    <div v-if="loading" class="map-loading">地图加载中…</div>
    <div ref="mapEl" class="amap-container"></div>
    <button v-if="!loading" class="label-toggle" @click="toggleLabels">
      {{ showLabels ? '隐藏名称' : '显示名称' }}
    </button>
    <div v-if="editable && !loading" class="map-tip">点击地图可选取地点</div>
  </div>
</template>

<style scoped>
.amap-wrap {
  position: relative;
  width: 100%;
  flex: 1;
  min-height: 0;
}
.amap-container {
  width: 100%;
  height: 100%;
  border-radius: var(--radius-sm);
  overflow: hidden;
}
.map-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 13px;
  background: rgba(var(--overlay-rgb), 0.03);
  border-radius: var(--radius-sm);
  z-index: 2;
}
.map-tip {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  padding: 5px 14px;
  border-radius: var(--radius-pill);
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 12px;
  pointer-events: none;
  z-index: 3;
}
.label-toggle {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 3;
  padding: 6px 14px;
  border-radius: var(--radius-pill);
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  backdrop-filter: blur(4px);
  border: none;
  cursor: pointer;
  transition: var(--transition);
}
.label-toggle:hover { background: rgba(0, 0, 0, 0.75); }
</style>
