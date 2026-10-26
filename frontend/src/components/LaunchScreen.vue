<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const emit = defineEmits(['enter'])
const globeEl = ref(null)
const globe = ref(null)
const stars = ref([])
const exiting = ref(false)
const introDone = ref(false)

function makeStars() {
  const arr = []
  for (let i = 0; i < 140; i++) {
    const isBig = Math.random() < 0.08
    arr.push({
      left: Math.random() * 100,
      top: Math.random() * 100,
      size: isBig ? 2.5 + Math.random() * 1.5 : 0.8 + Math.random() * 1.4,
      delay: Math.random() * 8,
      duration: 3 + Math.random() * 5,
      drift: (Math.random() - 0.5) * 40,
      glow: isBig
    })
  }
  return arr
}

function starStyle(s) {
  return {
    left: s.left + '%',
    top: s.top + '%',
    width: s.size + 'px',
    height: s.size + 'px',
    '--dur': s.duration + 's',
    '--delay': s.delay + 's',
    '--drift': s.drift + 'px',
    ...(s.glow ? { boxShadow: '0 0 6px rgba(255,255,255,0.7)' } : {})
  }
}

let rafId = null
let swayStart = 0
let exitTimer = null
let stepTimer = null

onMounted(async () => {
  stars.value = makeStars()

  const { default: Globe } = await import('globe.gl')
  initGlobe(Globe)

  swayStart = performance.now()
  rafId = requestAnimationFrame(animate)

  // 入场动画全部结束后释放按钮 transform，让 hover 生效
  setTimeout(() => { introDone.value = true }, 2600)
})

function initGlobe(Globe) {
  if (!globeEl.value) return

  const w = globeEl.value.clientWidth || window.innerWidth
  const h = globeEl.value.clientHeight || window.innerHeight

  globe.value = new Globe(globeEl.value, { animateIn: true })
    .globeImageUrl('/textures/earth-blue-marble.jpg')
    .bumpImageUrl('/textures/earth-topology.png')
    .width(w)
    .height(h)

  // 大气光晕：内置 atmosphere 蓝白色辉光
  globe.value.showAtmosphere(true)
    .atmosphereColor('#7dd3fc')
    .atmosphereAltitude(0.2)

  // 放慢自转；保留拖拽旋转与滚轮缩放操作
  const controls = globe.value.controls()
  controls.autoRotate = true
  controls.autoRotateSpeed = 0.2

  globe.value.pointOfView({ lat: 20, lng: 150, altitude: 1.5 })

  window.addEventListener('resize', onResize)
}

function animate(now) {
  if (globe.value && globe.value.rotation) {
    const t = (now - swayStart) / 1000
    // 极轻微俯仰摆动
    globe.value.rotation.z = Math.sin(t * 0.15) * 0.04
  }
  rafId = requestAnimationFrame(animate)
}

function onResize() {
  if (globe.value && globeEl.value) {
    const w = globeEl.value.clientWidth || window.innerWidth
    const h = globeEl.value.clientHeight || window.innerHeight
    globe.value.width(w).height(h)
  }
}

function handleEnter() {
  if (exiting.value) return
  exiting.value = true

  // 停止俯仰摆动与自动旋转；退出动画期间禁止拖拽/缩放，避免打断镜头
  cancelAnimationFrame(rafId)
  if (globe.value) {
    const controls = globe.value.controls()
    controls.autoRotate = false
    controls.enableRotate = false
    controls.enableZoom = false
    controls.enablePan = false

    // 第一步：先平移到中国视角（保持高度）
    globe.value.pointOfView({ lat: 34, lng: 108, altitude: 1.5 }, 600)

    // 第二步：锁定中国后镜头推进放大
    stepTimer = setTimeout(() => {
      if (globe.value) globe.value.pointOfView({ lat: 34, lng: 108, altitude: 0.4 }, 800)
    }, 600)
  }
  exitTimer = setTimeout(() => emit('enter'), 1450)
}

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  cancelAnimationFrame(rafId)
  clearTimeout(exitTimer)
  clearTimeout(stepTimer)
  if (globe.value) globe.value._destructor?.()
})
</script>

<template>
  <div class="launch-screen" :class="{ exiting }">
    <!-- 背景径向渐变：中心稍亮，四周深空黑 -->
    <div class="launch-bg"></div>

    <!-- 地球大气光晕层 -->
    <div class="globe-halo"></div>

    <div ref="globeEl" class="globe-container"></div>

    <!-- 星空粒子（前景层，放大地球时不被遮挡） -->
    <div class="star-layer">
      <span v-for="(s, i) in stars" :key="i" class="star" :style="starStyle(s)"></span>
    </div>

    <!-- 底部渐暗遮罩 -->
    <div class="launch-bg-gradient"></div>

    <div class="launch-content">
      <h2 class="en-title">Footprint Planet</h2>
      <h1 class="zh-title">我频繁记录着，因为人间值得。</h1>

      <button class="launch-btn" :class="{ 'intro-done': introDone }" @click="handleEnter">
        开始记录
      </button>

      <p class="launch-subtitle">记录每一次出发，点亮属于你的世界轨迹</p>
    </div>
  </div>
</template>

<style scoped>
.launch-screen {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: #050a1a;
  overflow: hidden;
}

/* ===== 背景径向渐变 ===== */
.launch-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    radial-gradient(circle at 50% 44%, rgba(56, 189, 248, 0.16) 0%, rgba(13, 19, 48, 0) 42%),
    radial-gradient(ellipse at 50% 45%, #0c1230 0%, #060b1f 58%, #020309 100%);
  transition: opacity 0.9s ease;
}
.launch-screen.exiting .launch-bg { opacity: 0; }

/* ===== 星空粒子（前景层） ===== */
.star-layer {
  position: absolute;
  inset: 0;
  z-index: 3;
  overflow: hidden;
  pointer-events: none;
  transition: opacity 1s ease;
}
.launch-screen.exiting .star-layer { opacity: 0; }

.star {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  animation: twinkle var(--dur) ease-in-out var(--delay) infinite;
  will-change: opacity, transform;
  pointer-events: none;
}
@keyframes twinkle {
  0%, 100% { opacity: 0.08; transform: translate(0, 0); }
  50% { opacity: 0.65; transform: translate(var(--drift), -5px); }
}

/* ===== 地球大气光晕 ===== */
.globe-halo {
  position: absolute;
  left: 50%;
  top: 50%;
  width: min(66vmin, 640px);
  height: min(66vmin, 640px);
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(125, 211, 252, 0.18) 0%, rgba(56, 189, 248, 0.07) 46%, transparent 68%);
  z-index: 1;
  pointer-events: none;
  transition: transform 1s cubic-bezier(0.4, 0, 0.2, 1), opacity 1s ease;
}
.launch-screen.exiting .globe-halo {
  transform: translate(-50%, -50%) scale(1.55);
  opacity: 0;
}

.globe-container {
  position: absolute;
  inset: 0;
  z-index: 2;
}
.globe-container :deep(canvas) {
  display: block;
}

/* 底部渐暗遮罩 */
.launch-bg-gradient {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 160px;
  background: linear-gradient(to top, rgba(5, 10, 26, 0.8), transparent);
  z-index: 3;
  pointer-events: none;
}

.launch-content {
  position: absolute;
  inset: 0;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  text-align: center;
  pointer-events: none;
  transition: opacity 0.5s ease, transform 0.5s ease;
}
.launch-screen.exiting .launch-content {
  opacity: 0;
  transform: translateY(-16px);
}

/* ===== 入场动画（顺序：地球→品牌名→标语→按钮） ===== */
.en-title {
  font-family: 'IBM Plex Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 28px;
  font-weight: 400;
  letter-spacing: 8px;
  text-transform: uppercase;
  background: linear-gradient(180deg, rgba(255,255,255,0.95), rgba(255,255,255,0.45));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  pointer-events: auto;
  opacity: 0;
  transform: translateY(-28px);
  animation: slideDown 0.9s cubic-bezier(0.22, 1, 0.36, 1) 0.7s forwards;
}

.zh-title {
  font-size: 36px;
  font-weight: 800;
  letter-spacing: 3px;
  color: var(--text-primary);
  text-shadow: 0 2px 30px rgba(0, 0, 0, 0.5);
  pointer-events: auto;
  opacity: 0;
  transform: translateY(14px);
  animation: fadeUp 0.9s cubic-bezier(0.22, 1, 0.36, 1) 1.0s forwards;
}

.launch-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 3px;
  opacity: 0;
  transform: translateY(14px);
  animation: fadeUp 0.8s cubic-bezier(0.22, 1, 0.36, 1) 1.7s forwards;
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-28px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== 开始按钮 ===== */
.launch-btn {
  background: transparent;
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.35);
  padding: 12px 44px;
  border-radius: var(--radius-pill);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
  pointer-events: auto;
  letter-spacing: 2px;
  backdrop-filter: blur(4px);
  opacity: 0;
  transform: translateY(14px);
  animation: fadeUp 0.8s cubic-bezier(0.22, 1, 0.36, 1) 1.3s forwards;
}

/* 入场动画结束后释放 transform，恢复 hover/active */
.launch-btn.intro-done {
  animation: none;
  opacity: 1;
  transform: translateY(0);
}

.launch-btn:hover {
  border-color: rgba(125, 211, 252, 0.85);
  background: rgba(56, 189, 248, 0.16);
  box-shadow: 0 0 40px rgba(56, 189, 248, 0.35);
  transform: translateY(0) scale(1.03);
}
.launch-btn:active {
  transform: translateY(0) scale(0.96);
}
</style>
