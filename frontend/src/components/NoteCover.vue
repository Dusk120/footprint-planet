<script setup>
defineProps({
  note: { type: Object, required: true },
  variant: { type: String, default: 'card' } // card | detail
})
</script>

<template>
  <img v-if="note?.image" class="cover-img" :src="note.image" :alt="note?.title || ''" />
  <div v-else class="cover-text" :class="`variant-${variant}`">
    <span v-if="note?.location" class="cover-loc">📍 {{ note.location.name }}</span>
    <h3 class="cover-title">{{ note?.title || '无标题' }}</h3>
    <span v-if="note?.author" class="cover-author">@{{ note.author }}</span>
  </div>
</template>

<style scoped>
.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-text {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 20px;
  text-align: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #10b981, #14b8a6);
}

/* 轻压暗四角，让文字在任何色相上都够清晰 */
.cover-text::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at center, transparent 35%, rgba(0, 0, 0, 0.28) 100%);
  pointer-events: none;
}

.cover-loc,
.cover-title,
.cover-author {
  position: relative;
  z-index: 1;
  color: #fff;
}

.cover-loc {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.5px;
  opacity: 0.85;
}

.cover-title {
  margin: 0;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: 0.5px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.25);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.cover-author {
  font-size: 12px;
  opacity: 0.8;
}

/* 卡片（社区列表 / 我的笔记）：4:3 竖版空间 */
.variant-card .cover-title {
  font-size: 19px;
}

/* 详情页：16:9 宽幅，标题可以更大 */
.variant-detail {
  gap: 14px;
  padding: 32px 48px;
}
.variant-detail .cover-loc {
  font-size: 13px;
}
.variant-detail .cover-title {
  font-size: 34px;
  -webkit-line-clamp: 2;
  max-width: 80%;
}
.variant-detail .cover-author {
  font-size: 14px;
}
</style>
