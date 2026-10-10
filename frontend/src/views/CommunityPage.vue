<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAppStore } from '../stores/app'
import CommunityStream from '../components/CommunityStream.vue'
import AuthorProfile from '../components/AuthorProfile.vue'
import NoteDetailPanel from '../components/NoteDetailPanel.vue'
import { useToast } from '../composables/useToast'

const store = useAppStore()
const { show: toast } = useToast()

const currentView = ref('stream') // 'stream' | 'author' | 'note'
const selectedNote = ref(null)
const selectedAuthor = ref(null)

const isStreamView = computed(() => currentView.value === 'stream')
const isAuthorView = computed(() => currentView.value === 'author')
const isNoteDetailView = computed(() => currentView.value === 'note')

// 作者主页是社区页内的视图，不单独开页面：换作者时把页码等状态一并重置（见 AuthorProfile 的 watch）
function handleAuthorClick(authorId) {
  // 兜底：调用方没传时用当前详情笔记的作者
  const id = authorId ?? selectedNote.value?.authorId
  if (id == null) {
    toast('笔记缺少作者信息，无法打开作者主页', 'error')
    return
  }
  selectedAuthor.value = id
  currentView.value = 'author'
}

function goBackToStream() {
  selectedAuthor.value = null
  selectedNote.value = null
  currentView.value = 'stream'
}

// 处理笔记点击（社区流和作者主页里都能点）
function handleNoteClick(note) {
  selectedNote.value = note
  currentView.value = 'note'
}

// 详情面板点赞：NoteDetailPanel 的 emit('like') 不带参数，
// 它持有的 selectedNote 与列表里是同一个对象引用，改了卡片会跟着变
async function handleLikeNote() {
  const note = selectedNote.value
  if (!note) return
  try {
    const res = await store.likeNote(note.id, !note.liked)
    note.likes = res.likes
    note.liked = res.liked
  } catch (e) {
    toast(e.message || '操作失败，请重试', 'error')
  }
}

// 从详情返回：从作者主页点进来的就回作者主页，否则回社区流
function handleBackFromNote() {
  selectedNote.value = null
  currentView.value = selectedAuthor.value ? 'author' : 'stream'
}

// 组件挂载时加载热门数据
onMounted(() => {
  store.loadHot()
})
</script>

<template>
  <div class="community-page">
    <!-- 社区流视图：用 v-show 而不是 v-if——进笔记详情时若把组件卸载，
         回来会重新挂载、页码/排序/搜索词全丢（返回后总是回到第一页） -->
    <CommunityStream
      v-show="isStreamView"
      @note-click="handleNoteClick"
    />

    <!-- 作者主页视图（页内，不跳页）：同理保留 TA的笔记/规划的页码 -->
    <AuthorProfile
      v-show="isAuthorView"
      :author-id="selectedAuthor"
      back-text="返回社区"
      @back="goBackToStream"
      @note-click="handleNoteClick"
    />

    <!-- 笔记详情视图：前两个视图用的是 v-show，这里不能再写 v-else-if -->
    <NoteDetailPanel
      v-if="isNoteDetailView"
      :note="selectedNote"
      :back-text="selectedAuthor ? '返回作者主页' : '返回社区'"
      :author-clickable="true"
      @back="handleBackFromNote"
      @author-click="handleAuthorClick"
      @like="handleLikeNote"
    />
  </div>
</template>
