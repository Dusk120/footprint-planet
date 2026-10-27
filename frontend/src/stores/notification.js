import { defineStore } from 'pinia'
import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import { getToken } from '../api/http'
import { notificationApi } from '../api/notification'
import { useToast } from '../composables/useToast'

// 通知字段与后端 NotificationVO / WebSocket payload 保持一致：
// { id, type, senderNickname, senderAvatar, noteId, noteTitle, content, isRead, createdAt }
export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  const unreadCount = ref(0)
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
    notifications.value.unshift(n)
    if (n.isRead !== 1) unreadCount.value += 1
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

  async function fetchNotifications({ page = 1, pageSize = 20 } = {}) {
    const res = await notificationApi.list({ page, pageSize })
    const fetched = res?.list || []
    const fetchedIds = new Set(fetched.map((x) => x.id))
    // 拉取期间通过 WebSocket 新到、且不在返回结果里的通知，避免被整体覆盖丢数据
    const pending = notifications.value.filter((x) => x.id != null && !fetchedIds.has(x.id))
    notifications.value = [...pending, ...fetched]
    // 未读数：以后端为准，再补上 pending 里未读的（后端快照尚未计入）
    const pendingUnread = pending.filter((x) => x.isRead !== 1).length
    unreadCount.value = (res?.unreadCount ?? 0) + pendingUnread
  }

  async function markRead(id) {
    await notificationApi.read(id)
    // 本地立即标记，避免红点闪烁
    const n = notifications.value.find((x) => x.id === id)
    if (n) n.isRead = 1
    // 重新拉取，用后端准确未读数纠正本地 unreadCount（WebSocket +1 与 fetch 覆盖存在竞态，
    // 本地 -1 可能残留虚高值；后端此刻已标记 is_read=1，返回的 unreadCount 才是准的）
    await fetchNotifications().catch(() => {})
  }

  async function markAllRead() {
    await notificationApi.readAll()
    notifications.value.forEach((n) => {
      n.isRead = 1
    })
    unreadCount.value = 0
  }

  return {
    notifications,
    unreadCount,
    connected,
    connect,
    disconnect,
    fetchNotifications,
    markRead,
    markAllRead,
  }
})
