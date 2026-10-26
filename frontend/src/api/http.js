// 统一请求封装：baseURL=/api，自动附带 token，统一解包 {code,message,data}
const TOKEN_KEY = 'footprint-planet:token'

export function getToken() {
  try { return localStorage.getItem(TOKEN_KEY) || '' } catch (e) { return '' }
}

export function setToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token)
    else localStorage.removeItem(TOKEN_KEY)
  } catch (e) { /* 隐私模式忽略 */ }
}

function buildQuery(params) {
  const sp = new URLSearchParams()
  Object.entries(params || {}).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') sp.set(k, v)
  })
  const s = sp.toString()
  return s ? `?${s}` : ''
}

async function request(path, { method = 'GET', body, params } = {}) {
  // FormData（文件上传）需由浏览器自动生成 multipart boundary，不能手动设 Content-Type
  const isForm = body instanceof FormData
  const headers = isForm ? {} : { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  let res
  try {
    res = await fetch(`/api${path}${buildQuery(params)}`, {
      method,
      headers,
      body: body !== undefined ? (isForm ? body : JSON.stringify(body)) : undefined
    })
  } catch (e) {
    throw new Error('网络连接失败，请确认后端服务已启动')
  }

  let data = null
  try { data = await res.json() } catch (e) { /* 空响应体 */ }

  if (!res.ok) {
    const err = new Error((data && data.message) || `请求失败（${res.status}）`)
    err.status = res.status
    err.code = (data && data.code) || res.status

    // 401 错误时清除 token 并触发登出
    if (res.status === 401) {
      setToken(null)
      // 触发全局登出
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('unauthorized'))
      }
    }

    throw err
  }

  if (data && typeof data === 'object' && 'code' in data) {
    if (data.code !== 0) {
      const err = new Error(data.message || '请求失败')
      err.status = res.status
      // 后端统一返回 HTTP 200 + 业务 code，所以 err.status 恒为 200，拿不到真实语义；
      // 这里把业务码也带上，调用方才能区分「接口不存在(404 资源不存在)」这类情况
      err.code = data.code
      // 业务码 401（未登录 / 登录过期 / 账号被禁用）同样要清 token 并触发全局登出，
      // 否则 token 失效后停留在当前页操作只会弹错误提示，不会自动跳回登录页
      if (data.code === 401) {
        setToken(null)
        if (typeof window !== 'undefined') {
          window.dispatchEvent(new CustomEvent('unauthorized'))
        }
      }
      throw err
    }
    return data.data
  }
  return data
}

export const http = {
  get: (path, params) => request(path, { params }),
  post: (path, body) => request(path, { method: 'POST', body }),
  put: (path, body) => request(path, { method: 'PUT', body }),
  delete: (path) => request(path, { method: 'DELETE' }),
  upload: (path, file) => {
    const fd = new FormData()
    fd.append('file', file)
    return request(path, { method: 'POST', body: fd })
  }
}
