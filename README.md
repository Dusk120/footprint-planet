# Footprint Planet · 足迹星球（后端服务）

「足迹星球」是一款旅行足迹 / 旅行社区 Web 应用：用户点亮去过的城市、按时间轴回顾旅行回忆、以地图驱动的方式编排旅游规划（支持多人实时协同）、在社区发布笔记并互相点赞评论，管理员在管理端进行用户、笔记与反馈治理。本仓库同时存放**后端服务**（根目录，Spring Boot）与**前端**（`frontend/`，Vue 3）。

- 项目类型：课程综合实践
- 运行环境：JDK 17、Maven 3.9+
- 基础路径：`/api`，认证方式：`Authorization: Bearer <token>`
- 接口契约：[`docs/足迹星球后端接口文档.md`](docs/足迹星球后端接口文档.md)（v2.0，共 **59 个接口**：49 用户端 + 10 管理端）

## 技术栈

| 分类 | 选型 |
| --- | --- |
| 框架 | Spring Boot 4.1.1（内嵌 Jackson 3）、Spring MVC |
| 持久层 | MyBatis-Plus `mybatis-plus-spring-boot4-starter` 3.5.17 + jsqlparser 分页插件 + 乐观锁（`@Version`） |
| 数据库 | MySQL 8+（utf8mb4，13 张表） |
| 缓存 | Redis（图形验证码、热门目的地 Top5、账号状态、高德算路结果），本机未启用不影响其它功能（验证码/热门/算路降级） |
| 安全 | JWT（jjwt 0.12.6）+ BCrypt（spring-security-crypto，仅加密不引入 Security 过滤器链） |
| 对象存储 | 阿里云 OSS（aliyun-sdk-oss 3.18.5，笔记配图 / 城市照片 / 头像） |
| 实时通信 | WebSocket + STOMP（`/ws`）：协作计划实时广播 + 点赞/评论/回复实时通知推送 |
| 地图服务 | 高德开放平台 Web 服务 API（`RestClient` 调用：路径规划算路，含真实路线 polyline） |
| 验证码 | hutool-captcha 5.8.47（`LineCaptcha` 生成，答案明文存 Redis，TTL 2 分钟，校验通过即删除） |
| 参数校验 | Bean Validation（jakarta.validation） |
| 接口风格 | RESTful，统一返回体 `{ code, message, data }` |

## 功能模块

**用户端**

- **账号认证**：图形验证码（`GET /api/captcha`，明文存 Redis）→ 注册 / 登录 / 登出；JWT 无状态鉴权，拦截器每次请求回库核对账号状态，`status=1` 的账号即时失效
- **个人中心**：资料与密码修改（含头像上传）、我的足迹与统计数据、我的笔记、我的规划
- **足迹点亮**：点亮 / 编辑 / 取消点亮城市；每座城市可设**主题色**（12 色板）、推荐指数、多次游玩记录（日期 + 照片）。前端地图按城市主题色整市涂色，直辖市按省级 adcode 归一
- **旅行回忆**：把各城市的游玩记录按时间轴展平返回，供前端「时间隧道」回顾；支持按城市名 / 省份搜索，按年份筛选
- **旅游规划（地图驱动 + 多人协同）**：以「地图选点 + 高德搜索」编排行程，行程按天组织（`plan_item` 表），相邻地点之间用**高德真实算路**保存出行方案（`plan_leg`，含沿道路的 polyline 路线）。支持**三级角色**（owner/editor/viewer）、**邀请链接**协作（签发/核销/撤销/编辑）、**乐观锁**并发控制（`version`）、协作锁定 / 截止时间、**STOMP 实时广播**（任一成员操作实时同步到所有在线协作者）。公开 / 私有、复制他人公开规划、作者公开规划列表（分页）
- **消息通知**：点赞 / 评论 / 回复触发**实时通知**（WebSocket 推送 + 落库），按「点赞 / 评论」分类，游标分页查询、单条 / 分类已读
- **社区笔记**：发布（配图走 OSS，地点须为本人已点亮城市）、列表分页 + 关键词搜索 + 最新 / 最热排序、笔记详情、点赞 / 取消点赞、删除自己的笔记
- **评论互动**：评论列表（**游标分页只作用于顶层评论**，回复随父评论返回）、发布评论 / 一级回复、评论与回复均可点赞、按权限删除；`liked` / `canDelete` 由后端按 JWT 算好下发
- **作者主页**：`GET /api/users/{id}` 返回资料 + 统计，配合公开笔记与公开规划列表
- **热门目的地**：按点亮人数动态统计 Top5（Redis 缓存 24h，新增 / 删除点亮时失效）
- **建议反馈**：匿名提交（内容 + 联系方式 + 截图）

**管理端**（`/api/admin/**`，需 `role=ADMIN`）

- **仪表盘**：用户 / 笔记 / 反馈总量与待处理数、笔记趋势、热门城市、热门笔记
- **用户管理**：列表（分页 / 搜索）、用户详情（含内容统计）、禁用 / 启用账号、重置密码
- **笔记管理**：全站笔记列表（分页 / 搜索）、删除违规笔记。**评论治理不另设接口**——管理员在社区页 / 笔记详情内即可删除任意评论（用户端删除评论接口的 `canDelete` 对管理员恒为 `true`）
- **反馈管理**：列表（按处理状态筛选）、标记已处理、删除

## 目录结构

```
Footprint_planet
├─ pom.xml / mvnw / mvnw.cmd
├─ docs/
│  └─ 足迹星球后端接口文档.md      # 接口契约（v2.0，59 个接口）
├─ sql/
│  └─ footprint_planet.sql         # 建库脚本 + 演示数据（v2.0，13 张表）
├─ frontend/                       # 前端（Vue 3 + Vite + Pinia + ECharts + globe.gl + 高德地图）
│  ├─ src/                         # 源码；node_modules/ 与 dist/ 已被 .gitignore 忽略
│  └─ vite.config.js               # dev 代理 /api → localhost:8080
└─ src/main/
   ├─ java/com/qzxx/footprint_planet
   │  ├─ FootprintPlanetApplication.java  # 启动类（@MapperScan 已配）
   │  ├─ common/         # Result / BusinessException / GlobalExceptionHandler / RedisKeys
   │  ├─ config/         # RedisConfig / MybatisPlusConfig（乐观锁插件）/ WebMvcConfiguration
   │  │                  # WebSocketConfig（STOMP /ws）/ RedisStartupCheck
   │  │                  # JwtProperties / OssProperties / SecurityConfig / OpenApiConfig
   │  ├─ context/        # BaseContext（ThreadLocal 持有当前用户 id / role）
   │  ├─ interceptor/    # JwtTokenInterceptor（解析 token、回库校验状态、管理端鉴权）
   │  │                  # JwtChannelInterceptor（WebSocket 握手鉴权）
   │  ├─ controller/     # 17 个 REST 控制器
   │  ├─ service/        # Service 接口 + impl/ 下实现（OssService 直接实现为类，无独立接口）
   │  ├─ mapper/         # 13 个 BaseMapper 子接口（对应 13 张表）
   │  ├─ dto/request/    # 24 个入参对象（jakarta 校验注解）
   │  ├─ vo/             # 27 个出参对象（含泛型分页 PageVO<T>）
   │  ├─ entity/         # 13 个实体（camelCase ↔ 下划线自动映射）
   │  └─ utils/          # JwtUtil（签发 / 解析）
   └─ resources
      ├─ application.yml              # 公共配置（占位符引用 + 默认 dev profile）
      ├─ application-dev.yml.example  # ★ 本地配置模板（MySQL / Redis / JWT / OSS / 高德 / 前端地址）
      └─ application-dev.yml          # 由模板复制而来，已被 .gitignore 忽略
```

## 核心约定

- **统一返回体**：成功 `{ code: 0, message: "ok", data: ... }`；**失败 HTTP 状态码也一律是 200**，用 `code` 表示错误类别 —— 前端 `http.js` 据此解包，并在抛出的 error 上附带 `err.code`，以便区分业务码与 HTTP 码。
- **错误码**：`400` 参数 / 业务错误、`401` 未认证、`403` 无权限、`404` 不存在、`409` 乐观锁冲突（计划已被他人修改）、`500` 服务器异常。
- **权限**：普通接口需登录（JWT）；管理端按**路径前缀** `/api/admin/` 统一校验 `role=ADMIN`，新增管理端接口自动生效。
- **公开读接口**：笔记列表 / 详情、评论列表、他人主页、公开规划详情、热门目的地等允许匿名访问，带 token 时会额外算出 `liked` / `canDelete` / `myRole` 等与「当前用户」相关的字段；账号被禁用时按匿名处理。
- **密码**：BCrypt 加密存储；接口响应从不回传 `password`。
- **JSON 列**：`note.photos`、`visited_city.records`、`feedback.images` 以 JSON 存储，走 `JacksonTypeHandler` 读写 `List`；旅游规划的行程**不再用 JSON 大字段**，改为独立表 `plan_item` / `plan_leg`。
- **分页（两套）**：
  - 翻页式 `PageVO { list, total, page, pageSize }`：用于规划列表、笔记列表、作者公开规划、管理端列表。
  - **游标式**：评论列表 `CommentPageVO { list, nextCursor, hasMore }`、通知列表 `NotificationPageVO { list, nextCursor, hasMore, unreadCount, totalUnreadCount }`——统一用 `lt(id, cursor)` + `orderByDesc(id)`，前端滚动加载只消费 `nextCursor` / `hasMore`。
- **旅游规划三级角色**：`owner`（`plan.user_id` 判定）/ `editor` / `viewer`（`plan_collaborator` 表）。权限校验顺序：角色 → 协作状态（locked/deadline）→ 写入。条目增删改 / 排序 / 交通段 / 邀请签发走 `requireEditor`（editor 可）；删计划 / 封面 / 锁定 / 截止 / 移除成员 / 撤销与更新邀请走 `requireOwner`。
- **乐观锁**：`Plan.version` 加 `@Version`，所有 `updateById(plan)` 自动 `where version=旧值`，`rows=0` 抛 409；`reorderItems` 特殊（前端传 version，走手动 CAS）。
- **外键与级联**：评论树用自引用外键实现一级回复，删除笔记 / 评论时其回复与点赞由 `ON DELETE CASCADE` 自动清理；计划删除时 items / legs / collaborators / invites 由级联清理，应用层不必手写递归删除。

## 数据库

建库脚本为 `sql/footprint_planet.sql`（v2.0，**13 张表**，含演示数据）。脚本开头是 `DROP DATABASE IF EXISTS footprint_planet;` 接 `CREATE DATABASE` + `USE`，**即整库重建，不需要手工建库**。

> ⚠️ 正因为它会 DROP 同名库，**导入前请确认 `footprint_planet` 里没有需要保留的数据**（换机器重装、或想拿演示数据覆盖时正好适用）。

Windows 命令行导入（务必显式指定字符集，否则中文乱码）：

```bash
mysql -uroot -p --default-character-set=utf8mb4 < sql/footprint_planet.sql
```

| 表 | 作用 |
| --- | --- |
| `user` | 用户（`role`: ADMIN/USER；`status`: 0 正常 / 1 禁用） |
| `visited_city` | 点亮过的城市，`records` 存旅行回忆，`color` 存主题色 |
| `plan` | 旅游规划（含 `version` 乐观锁、`collab_locked`、`collab_deadline`） |
| `plan_item` | 行程地点条目（第几天 `day_no`、顺序 `sort_no`、坐标、POI id、备注） |
| `plan_leg` | 相邻条目交通段（方式、时长、距离、摘要、真实路线 `polyline`） |
| `plan_collaborator` | 计划协作者（人 ↔ 计划 ↔ 角色 editor/viewer） |
| `plan_invite` | 计划邀请链接（token、角色、次数上限、过期时间、撤销状态） |
| `note` | 社区笔记（`photos` 为 JSON，冗余 `author_nickname` / `location_*` 便于列表展示） |
| `note_like` | 笔记点赞关系（`(note_id, user_id)` 唯一，保证一人一赞） |
| `note_comment` | 评论；`parent_id` **自引用** `note_comment(id)`，非空即一级回复 |
| `comment_like` | 评论（含回复）点赞关系（`(comment_id, user_id)` 唯一） |
| `notification` | 消息通知（点赞/评论/回复，`is_read` 标记已读） |
| `feedback` | 建议反馈（匿名提交，不记录用户；`handled`: 处理状态） |

**演示账号**（密码均为 `123456`）：`admin`（管理员，角色 ADMIN）、`xiaoA`、`zhangsan`、`zhuiguangzhe`、`xinghe`（普通用户，角色 USER）。

## 本地启动

### 1. 准备

- JDK 17、Maven 3.9+；
- MySQL 已导入 `sql/footprint_planet.sql`（脚本会自动 DROP + CREATE `footprint_planet`，无需手工建库）；
- Redis 可选（不启动时验证码、热门城市缓存、算路缓存不可用，其余功能正常）；
- 图片上传依赖阿里云 OSS，未配置密钥时上传接口不可用（不影响登录等其它功能）；
- 出行方案算路依赖高德 Web 服务 key，未配置时算路失败、地图回退直线（不影响其它功能）。

### 2. 配置账号密码（关键）

所有环境相关的敏感配置集中在 `application-dev.yml`，该文件**已被 `.gitignore` 忽略**（仓库里只有模板）。首次运行先复制模板再填写：

```bash
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
```

需要填写的项（模板里已逐项留注释）：

| 配置项 | 说明 |
| --- | --- |
| `footprint.datasource.*` | MySQL 连接：`host` / `port` / `database` / `username` / `password`（`driver-class-name` 模板已给默认值） |
| `footprint.redis.*` | Redis 连接：`host` / `port` / `password`（本机无密码可留空） |
| `footprint.jwt.secret-key` / `ttl` | JWT 签名密钥与有效期（毫秒，模板默认 7200000 = 2 小时），生产部署请更换密钥 |
| `footprint.frontend-url` | 前端地址（拼邀请链接 `link` 用），本地默认 `http://localhost:5173` |
| `footprint.amap.web-key` | 高德 **Web 服务** key（后端算路用，注意不是前端「Web 端(JS API)」key）。不填也能启动，只是「出行方案」算路降级 |
| `aliyun.oss.*` | OSS `endpoint` / `access-key-id` / `access-key-secret` / `bucket-name` / `url-prefix` |

- `application.yml` 只放公共配置，通过 `${footprint.*}` 占位符引用上述值，并默认激活 `dev` profile；部署新环境时复制一份 `application-{env}.yml` 再切换即可。

### 3. 运行

```bash
./mvnw spring-boot:run        # Windows CMD / PowerShell 用 mvnw.cmd spring-boot:run
# 或 IDE 中直接运行 com.qzxx.footprint_planet.FootprintPlanetApplication
```

启动成功后服务监听 `http://localhost:8080`。

> 若本机存在 `SERVER__PORT` 环境变量（如某些 IDE 注入），会被 Spring relaxed binding 当成 `server.port` 抢占端口，请显式指定 `--server.port=8080` 规避。

### 4. 验证

登录**必须携带图形验证码**（先取 uuid，再拿该 uuid 对应的明文；正常流程是人工识别图片里的字符）：

```bash
# 1) 取验证码，返回 { uuid, img }，img 是可直接塞进 <img src> 的 data URI
curl -s http://localhost:8080/api/captcha

# 2) 开发环境图省事，直接从 Redis 读明文（Redis 不可用时该接口会报错）
#    答案 TTL 仅 2 分钟，且校验通过后立即删除，需在两步之间尽快完成
redis-cli GET captcha:<uuid>

# 3) 登录
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456","uuid":"<uuid>","code":"<code>"}'
# => {"code":0,"message":"ok","data":{"token":"...","user":{...}}}
```

## 关联资源

- 前端：[`frontend/`](frontend/)（Vue 3 + Vite + Pinia + ECharts + globe.gl + 高德地图 + STOMP）。启动：`cd frontend && npm install && npm run dev`，访问 `http://localhost:5173`；Vite 已配代理 `/api` → `localhost:8080`，需后端先启动
- 接口文档：[`docs/足迹星球后端接口文档.md`](docs/足迹星球后端接口文档.md)（v2.0，59 个接口）
- 建库脚本：[`sql/footprint_planet.sql`](sql/footprint_planet.sql)（v2.0，13 张表 + 演示数据）

## License

本项目仅供学习交流使用。
