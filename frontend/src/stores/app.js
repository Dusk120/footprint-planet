import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, userApi, visitedApi, planApi, noteApi, hotApi, getToken, setToken } from '../api'
import { useNotificationStore } from './notification'

const THEME_KEY = 'footprint-planet:theme'

function defaultUser() {
  return {
    nickname: '', avatar: '', motto: '',
    visitedCities: 0, visitedProvinces: 0, totalKm: 0, totalDays: 0,
    role: 'USER'
  }
}

// 后端字段 cityName <-> 前端字段 name 的映射
function fromApiVisited(c) {
  if (!c) return c
  return { ...c, name: c.cityName ?? c.name }
}
function toApiVisited(c) {
  const { name, ...rest } = c
  return { ...rest, cityName: name ?? c.cityName }
}

export const useAppStore = defineStore('app', () => {
  // === 主题（前端本地偏好） ===
  const theme = ref(localStorage.getItem(THEME_KEY) || 'dark')
  function applyTheme(t) {
    document.documentElement.setAttribute('data-theme', t)
  }
  function toggleTheme() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
    localStorage.setItem(THEME_KEY, theme.value)
    applyTheme(theme.value)
  }
  applyTheme(theme.value)

  // === 认证 ===
  const isLoggedIn = ref(!!getToken())
  const currentUser = ref(null)
  const user = ref(defaultUser())

  const isAdmin = computed(() => user.value.role === 'ADMIN')

  function setUserData(u) {
    currentUser.value = u || null
    user.value = { ...defaultUser(), ...(u || {}) }
  }

  function resetData() {
    visitedCityList.value = []
    hotDestinations.value = []
    user.value = defaultUser()
    currentUser.value = null
  }

  async function login({ username, password, uuid, code }) {
    const res = await authApi.login({ username, password, uuid, code })
    setToken(res.token)
    isLoggedIn.value = true
    setUserData(res.user)
    await loadLoginData()
    useNotificationStore().connect()
    return true
  }

  async function register({ username, password, nickname, uuid, code }) {
    const res = await authApi.register({ username, password, nickname, uuid, code })
    setToken(res.token)
    isLoggedIn.value = true
    setUserData(res.user)
    await loadLoginData()
    useNotificationStore().connect()
    return true
  }

  async function logout() {
    try { await authApi.logout() } catch (e) { /* 忽略 */ }
    setToken(null)
    isLoggedIn.value = false
    resetData()
    useNotificationStore().disconnect()
  }

  // 应用启动时恢复登录态。只有「拿不到当前用户」才说明 token 失效
  async function bootstrap() {
    if (!getToken()) return
    let u
    try {
      u = await authApi.me()
    } catch (e) {
      setToken(null)
      isLoggedIn.value = false
      resetData()
      return
    }
    setUserData(u)
    useNotificationStore().connect()
    // 足迹列表是次要数据，单独 try：原来它和 /auth/me 共用一个 catch，
    // 后端重启或网络抖动时刷新页面，会被当成 token 失效直接登出
    try {
      await loadVisited()
    } catch (e) { /* 统计先留 0，不影响登录态 */ }
  }

  // 单飞的初始化：main.js 与路由守卫共用，避免重复请求 /auth/me
  let initTask = null
  function ensureInit() {
    if (!getToken()) return Promise.resolve(false)
    if (!initTask) {
      initTask = bootstrap().finally(() => { initTask = null })
    }
    return initTask
  }

  async function updateProfile(payload) {
    const u = await userApi.updateMe(payload)
    setUserData(u)
    return { success: true }
  }

  // === 业务数据 ===
  const visitedCityList = ref([])
  const hotDestinations = ref([])

  function refreshUserStats() {
    user.value.visitedCities = visitedCityList.value.length
    user.value.visitedProvinces = new Set(visitedCityList.value.map(c => c.province).filter(Boolean)).size
  }

  async function loadVisited() {
    const list = await visitedApi.list()
    visitedCityList.value = (list || []).map(fromApiVisited)
    refreshUserStats()
  }

  // 我的规划统一走后端真分页：只取当前页，返回 { list, total }
  async function fetchPlans({ page, pageSize } = {}) {
    const res = await planApi.list({ page, pageSize })
    return { list: res?.list || [], total: res?.total || 0 }
  }

  // 作者公开规划：同样后端真分页
  async function fetchAuthorPlans(authorId, { page, pageSize } = {}) {
    const res = await userApi.getPublicPlans(authorId, { page, pageSize })
    return { list: res?.list || [], total: res?.total || 0 }
  }

  // 旅行回忆：keyword 走后端模糊查询（城市名/省份），不传则返回全量展平列表
  async function fetchMemories({ keyword } = {}) {
    const params = {}
    if (keyword) params.keyword = keyword
    const res = await visitedApi.memories(params)
    return (res || []).map(m => ({ ...m, name: m.cityName ?? m.name }))
  }

  // sort=latest 最新（默认）| hot 按点赞数；keyword 由后端对标题/内容/作者昵称/地点模糊匹配
  function noteQuery({ sort, keyword, authorId, page, pageSize } = {}) {
    const params = {}
    if (sort) params.sort = sort
    if (keyword) params.keyword = keyword
    if (authorId) params.authorId = authorId
    if (page) params.page = page
    if (pageSize) params.pageSize = pageSize
    return params
  }

  // 笔记列表统一走后端真分页：只取当前页，返回 { list, total }
  async function fetchNotes({ sort, keyword, authorId, page, pageSize } = {}) {
    const res = await noteApi.list(noteQuery({ sort, keyword, authorId, page, pageSize }))
    return { list: res?.list || [], total: res?.total || 0 }
  }

  async function loadHot() {
    hotDestinations.value = await hotApi.list() || []
  }

  async function loadLoginData() {
    await loadVisited()
  }

  // === 足迹点亮 ===
  async function addVisitedCity(city) {
    const created = await visitedApi.add(toApiVisited(city))
    visitedCityList.value.push(fromApiVisited(created))
    refreshUserStats()
    return fromApiVisited(created)
  }

  async function updateVisitedCity(city) {
    const id = city.id
    const updated = await visitedApi.update(id, toApiVisited(city))
    const idx = visitedCityList.value.findIndex(c => c.id === id)
    if (idx >= 0) visitedCityList.value[idx] = fromApiVisited(updated)
    return true
  }

  async function removeVisitedCity(city) {
    await visitedApi.remove(city.id)
    visitedCityList.value = visitedCityList.value.filter(c => c.id !== city.id)
    refreshUserStats()
    return true
  }

  // === 旅游规划（写操作后由页面重新拉取当前页，不维护本地副本） ===
  async function addPlan(item) {
    return await planApi.create(item)
  }

  async function removePlan(id) {
    await planApi.remove(id)
  }

  async function updatePlan(id, data) {
    return await planApi.update(id, data)
  }

  // 将作者分享的公开规划复制一份到自己的旅游规划
  // （后端校验源规划必须是公开的；副本同样公开，这是产品上定下的语义，别再改成私有）
  async function copyAuthorPlan(plan) {
    return await planApi.copy(plan.id)
  }

  // === 社区笔记 ===
  async function publishNote(note) {
    return await noteApi.create(note)
  }

  async function removeNote(noteId) {
    await noteApi.remove(noteId)
  }

  async function likeNote(noteId, like) {
    return await noteApi.like(noteId, like)
  }

  // === 评论管理 ===
  async function getComments(noteId, params) {
    return await noteApi.getComments(noteId, params)
  }

  // parentId 为空 = 顶层评论；非空 = 回复该顶层评论（只支持一级回复）
  async function createComment(noteId, content, parentId = null) {
    return await noteApi.createComment(noteId, { content, parentId })
  }

  async function removeComment(noteId, commentId) {
    await noteApi.removeComment(noteId, commentId)
  }

  // 评论与回复共用
  async function likeComment(noteId, commentId, like) {
    return await noteApi.likeComment(noteId, commentId, like)
  }

  // === 作者主页（按 userId 每次实时拉取，不缓存） ===
  async function loadAuthorProfile(authorId) {
    return await userApi.getProfile(authorId)
  }

  // 判断笔记是否本人所发：优先按 authorId（nickname 可重复，仅作无 authorId 时的兜底）
  function isMyNote(note) {
    if (!note) return false
    const myId = user.value?.id
    if (note.authorId != null && myId != null) return note.authorId === myId
    const myNick = user.value?.nickname
    return !!myNick && note.author === myNick
  }

  return {
    isLoggedIn, currentUser, user, isAdmin,
    login, register, logout, bootstrap, ensureInit, updateProfile,
    theme, toggleTheme,
    visitedCityList, addVisitedCity, updateVisitedCity, removeVisitedCity,
    fetchMemories,
    fetchPlans, fetchAuthorPlans, addPlan, removePlan, updatePlan,
    copyAuthorPlan,
    hotDestinations, publishNote, removeNote, likeNote,
    getComments, createComment, removeComment, likeComment,
    loadAuthorProfile, isMyNote,
    loadHot, fetchNotes
  }
})
