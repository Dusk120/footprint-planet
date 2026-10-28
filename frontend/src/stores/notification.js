import { defineStore } from 'pinia'
import { ref, reactive, computed } from 'vue'
import { Client } from '@stomp/stompjs'
import { getToken } from '../api/http'
import { notificationApi } from '../api/notification'
import { useToast } from '../composables/useToast'

// 通知字段与后端 NotificationVO / WebSocket payload 保持一致：
// { id, type, senderNickname, senderAvatar, noteId, noteTitle, content, isRead, createdAt }

const PAGE_SIZE = 8

// type 分类：后端 4 种细类型，前端归为「点赞 / 评论」两个 tab
const LIKE_TYPES = ['like_note', 'like_comment']
const COMMENT_TYPES = ['comment', 'reply']

function tabOf(type) {
  return LIKE_TYPES.includes(type) ? 'like' : 'comment'
}

export const useNotificationStore = defineStore('notification', () => {
  // 两个 tab 各自的游标分页状态（列表数据、游标、是否还有、加载中、是否已加载过）
  const tabs = reactive({
    like: { items: [], cursor: null, hasMore: true, loading: false, loaded: false },
    comment: { items: [], cursor: null, hasMore: true, loading: false, loaded: false },
  })
  const activeTab = ref('like')
  // 分 tab 未读数；总未读（铃铛红点）= 两者之和
  const unread = reactive({ like: 0, comment: 0 })
  const totalUnread = computed(() => unread.like + unread.comment)
  const connected = ref(false)

  const { show } = useToast()
  let client = null

  function onMessage(msg) {
    let n
    try {
      n = JSON.parse(msg.body)
    } catch (e) {
      return
    }
    const tab = tabOf(n.type)
    if (n.isRead !== 1) unread[tab] += 1
    // 若该 tab 已加载过，把新通知插到头部；游标分页是「id 更小才往后查」，
    // 头部插入的新数据 id 更大，不会被后续翻页重复查到，不会破坏游标
    const s = tabs[tab]
    if (s.loaded) s.items.unshift(n)
    show(`${n.senderNickname || '有人'}：${n.content || '有新通知'}`, 'info')
  }

  function connect() {
    const token = getToken()
    if (!token || client) return
    const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
    client = new Client({
      brokerURL: `${protocol}://${location.host}/ws`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        connected.value = true
        client.subscribe('/user/queue/notifications', onMessage)
      },
      onDisconnect: () => {
        connected.value = false
      },
      onStompError: () => {
        connected.value = false
      },
    })
    client.activate()
  }

  function disconnect() {
    if (client) {
      try {
        client.deactivate()
      } catch (e) {
        // 忽略关闭异常
      }
      client = null
    }
    connected.value = false
  }

  // 拉取某一 tab：reset=true 拉第一页（清空重载），否则按游标加载下一页
  async function fetchTab(tab, { reset = false } = {}) {
    const s = tabs[tab]
    if (s.loading) return
    if (!reset && !s.hasMore) return
    s.loading = true
    try {
      const res = await notificationApi.list({
        type: tab,
        cursor: reset ? null : s.cursor,
        pageSize: PAGE_SIZE,
      })
      if (reset) s.items = []
      s.items.push(...(res?.list || []))
      s.cursor = res?.nextCursor ?? null
      s.hasMore = res?.hasMore === true
      s.loaded = true
      // 未读数以后端为准；后端只给「当前 type 未读 + 全局未读」，另一个 type 用差值反推
      const cur = res?.unreadCount ?? 0
      const total = res?.totalUnreadCount ?? 0
      const other = tab === 'like' ? 'comment' : 'like'
      unread[tab] = cur
      unread[other] = Math.max(0, total - cur)
    } finally {
      s.loading = false
    }
  }

  async function markRead(id, tab) {
    await notificationApi.read(id)
    const n = tabs[tab].items.find((x) => x.id === id)
    if (n && n.isRead !== 1) {
      n.isRead = 1
      unread[tab] = Math.max(0, unread[tab] - 1)
    }
  }

  async function markAllRead(tab) {
    await notificationApi.readAll(tab)
    tabs[tab].items.forEach((n) => {
      n.isRead = 1
    })
    unread[tab] = 0
  }

  return {
    tabs,
    activeTab,
    unread,
    totalUnread,
    connected,
    connect,
    disconnect,
    fetchTab,
    markRead,
    markAllRead,
  }
})
