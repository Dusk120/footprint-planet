# 足迹星球 · 前端

「足迹星球」旅行社区的前端工程（Vue 3）。与根目录的 Spring Boot 后端配套，通过 REST + WebSocket（STOMP）交互。后端接口契约见 [`../docs/足迹星球后端接口文档.md`](../docs/足迹星球后端接口文档.md)。

## 技术栈

| 分类 | 选型 |
| --- | --- |
| 框架 | Vue 3.5（Composition API + `<script setup>`） |
| 构建 | Vite 8 |
| 状态 | Pinia 4 |
| 路由 | Vue Router 5（history 模式） |
| 地图 | 高德 JSAPI 2.0（`@amap/amap-jsapi-loader`）、ECharts 5、globe.gl |
| 实时通信 | `@stomp/stompjs`（原生 WebSocket，无 SockJS） |
| 拖拽 | vuedraggable |
| 代码规范 | ESLint + Prettier |

## 环境要求

- Node.js ≥ 20.19（推荐 22 LTS，Vite 8 对 Node 版本有最低要求）
- 后端已启动并监听 `http://localhost:8080`（前端 dev server 会把 `/api`、`/ws` 代理过去）

## 快速开始

```bash
npm install        # 安装依赖
npm run dev        # 启动开发服务器，访问 http://localhost:5173
npm run build      # 生产构建，产物在 dist/
npm run preview    # 本地预览构建产物
npm run lint       # ESLint 检查并自动修复（src 下 .vue/.js/.ts）
npm run format     # Prettier 格式化 src
```

## 目录结构

```
frontend/
├─ vite.config.js          # Vite 配置：@ 别名、/api 与 /ws 代理
├─ index.html              # 入口 HTML
└─ src/
   ├─ main.js              # 应用入口：注册 Pinia/Router，恢复登录态，监听 401 跳登录
   ├─ App.vue              # 根组件
   ├─ api/                 # 后端接口封装
   │  ├─ http.js           # 统一请求：baseURL=/api、自动带 token、解包 {code,message,data}
   │  └─ auth/user/note/comment/plan/visited/notification/hot/feedback/admin/upload.js
   ├─ router/index.js      # 路由表 + 全局守卫（requireAuth / requireAdmin）
   ├─ stores/
   │  ├─ app.js            # 登录态、当前用户、isAdmin
   │  └─ notification.js   # 通知列表（游标分页）+ STOMP 连接与订阅
   ├─ views/               # 页面（含 views/admin/ 管理端）
   ├─ components/          # 通用组件（含 layout/ 布局、admin/ 管理端组件）
   ├─ composables/         # 组合式逻辑（useToast 等）
   ├─ utils/               # 工具（cityColor 城市配色、format 格式化）
   ├─ data/cities.js       # 城市数据（后端 city-dict.json 由它生成）
   └─ styles/              # 全局样式（global.css / admin.css）
```

## 关键配置

### 开发代理（vite.config.js）

```js
proxy: {
  '/api': { target: 'http://localhost:8080', changeOrigin: true },
  '/ws':  { target: 'http://localhost:8080', ws: true },
}
```

- 前端请求统一走 `/api` 前缀，由 dev server 代理到后端 8080，无需处理 CORS。
- WebSocket 端点 `/ws` 也由代理转发；生产部署时需让 `/api` 与 `/ws` 指向同一后端域名。

### 高德地图 key

key 与安全密钥硬编码在 `src/components/AmapMap.vue` 顶部：

```js
const AMAP_KEY = '...'      // 前端 JSAPI key（地图展示/选点）
const AMAP_SECURITY = '...' // 安全密钥（securityJsCode，勾选「安全密钥」才需填）
```

注意：这是**前端 JSAPI key**，与后端算路用的「Web 服务 key」（`footprint.amap.web-key`）不是同一个。更换 key 时改这里即可（如需接入环境变量，可改为 `import.meta.env.VITE_AMAP_KEY`）。

## 与后端对接约定

### 统一返回体

所有接口返回 `{ code, message, data }`，`code = 0` 表示成功，否则 `data` 为 null。`src/api/http.js` 已统一解包：成功直接返回 `data`，失败抛错并在 `err.code` 上附带业务码。

错误码：`400` 参数/业务错误、`401` 未认证（或账号被禁用）、`403` 无权限、`404` 不存在、`409` 乐观锁冲突、`500` 服务器异常。

### 鉴权

- token 存于 `localStorage`，key 为 `footprint-planet:token`。
- 请求头统一带 `Authorization: Bearer <token>`。
- 收到 401（HTTP 401 或业务码 401）时自动清除 token 并派发 `unauthorized` 事件，路由守卫据此跳回登录页。

### 实时通信（STOMP）

- 连接地址：`/ws`（`connectHeaders` 带 `Authorization: Bearer <token>`）。
- 通知订阅：`/user/queue/notifications`（`src/stores/notification.js`）。
- 计划协作订阅：`/topic/plan/{planId}`（`src/views/PlanEditorPage.vue`），实时接收成员操作广播。

## 代码规范

- ESLint + `eslint-plugin-vue`，配置见 `.eslintrc.js`；Prettier 配置见 `.prettierrc`。
- 提交前建议 `npm run lint && npm run format`。
