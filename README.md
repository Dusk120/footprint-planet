# Footprint Planet · 足迹星球（后端服务）

「足迹星球」是一款旅行足迹 / 旅行社区 Web 应用的**后端服务**：用户可以点亮去过城市、记录旅游规划、发布笔记并互相点赞，管理员可在管理端进行用户、笔记与反馈管理。配套前端见 `footprint-planet`（Vue 3）仓库。

- 项目类型：课程综合实践
- 运行环境：JDK 17、Maven 3.9+
- 基础路径：`/api`，认证方式：`Authorization: Bearer <token>`

## 技术栈

| 分类 | 选型 |
| --- | --- |
| 框架 | Spring Boot 4.1.1（内嵌 Jackson 3）、Spring MVC |
| 持久层 | MyBatis-Plus `mybatis-plus-spring-boot4-starter` 3.5.17 + jsqlparser 分页插件 |
| 数据库 | MySQL 8+（utf8mb4） |
| 缓存 | Redis（`spring-boot-starter-data-redis`，预留；本机未启用也可正常启动） |
| 安全 | JWT（jjwt 0.12.6）+ BCrypt（spring-security-crypto，仅加密不引入 Security 过滤器链） |
| 对象存储 | 阿里云 OSS（aliyun-sdk-oss 3.18.5，图片上传） |
| 参数校验 | Bean Validation（jakarta.validation） |
| 接口风格 | RESTful，统一返回体 `{ code, message, data }` |

## 当前进度

> 说明：项目采用「分层骨架先行、业务后置」的推进方式。以下均已落地并通过 `mvn test`：

- [x] 分层包结构（Controller / Service / ServiceImpl / Mapper / DTO / VO / 实体）
- [x] 配置重构：`application.yml`（公共/占位） + `application-dev.yml`（账号密码集中）双 profile
- [x] 公共件：`Result` 统一返回、`BusinessException`、`GlobalExceptionHandler` 全局异常处理
- [x] 基础设施配置：`RedisConfig`（Jackson 3 序列化）、`MybatisPlusConfig`（分页插件）、`OssProperties` + `OssService` + `UploadController`
- [x] 6 个 Mapper、7 组 Service 接口与空实现、11 个 Controller 空壳（路由前缀就位）
- [ ] 业务逻辑实现：认证（登录/注册/JWT 拦截器含 role/status 校验）、笔记/点赞、管理端统计等
- [ ] Jackson 时间格式统一为 `yyyy-MM-dd HH:mm:ss`

> 接口契约以配套文档《足迹星球后端接口文档》(v1.2) 为准。

## 功能模块

**用户端**

- 认证：注册 / 登录（Bearer token）
- 个人中心：资料与密码修改、我的足迹与统计数据、他人主页
- 足迹城市：点亮 / 编辑 / 删除去过城市，地图联动
- 旅游规划：增删改查，支持公开规划浏览
- 社区笔记：发布（配图走 OSS）、列表分页、点赞 / 取消点赞、热门笔记
- 热门目的地：按点亮人数动态统计 Top 5（无需额外表）
- 建议反馈：提交反馈 / 查看处理结果

**管理端**（`/api/admin/**`，需 role=ADMIN）

- 仪表盘：用户数 / 笔记数 / 反馈数 / 待处理数 / 趋势 / 热门城市 / 热门笔记
- 用户管理：列表、禁用 / 启用（`status` 0/1）、重置密码
- 笔记管理：全量列表、删除
- 反馈管理：列表（含用户信息）、标记处理

## 目录结构

```
Footprint_planet
├─ pom.xml
└─ src/main/java/com/qzxx/footprint_planet
   ├─ FootprintPlanetApplication.java     # 启动类（@MapperScan 已配）
   ├─ common/        # Result / BusinessException / GlobalExceptionHandler
   ├─ config/        # RedisConfig / MybatisPlusConfig / OssProperties
   ├─ controller/    # REST 路由（/api/... 域前缀）
   ├─ service/       # Service 接口 + impl/ 实现
   ├─ mapper/        # BaseMapper 子接口（对应 6 张表）
   ├─ dto/request/   # 入参 Record（jakarta 校验注解）
   ├─ vo/            # 出参 Record（含泛型分页 PageVO<T>）
   └─ entity/        # 实体（camelCase ↔ 下划线自动映射）
src/main/resources
   ├─ application.yml      # 公共配置 + 占位符 + JWT 密钥
   └─ application-dev.yml  # ★ 本地账号密码集中填写处（MySQL / Redis / OSS）
src/test/java/...          # 上下文装配测试
```

## 核心约定

- **统一返回体**：成功 `{ code: 0, message: "ok", data: ... }`；失败 HTTP 一律 200、`code` 非 0 表示错误类别，与前端 `http.js` 解包约定吻合。
- **错误码**：`400` 参数/业务错误、`401` 未认证、`403` 无权限、`404` 不存在、`500` 服务器异常。
- **权限**：普通接口需登录（JWT），管理端接口额外校验 `role=ADMIN`；`status=1` 的禁用账号不可登录。
- **密码**：BCrypt 加密存储；接口响应从不回传 `password`。
- **JSON 列**：`note.photos`、`plan.days/waypoints`、`visited_city.records` 以 JSON 存储，走 `JacksonTypeHandler` 读写 `List` / `List<Map>`。
- **分页**：`PageVO { list, total, page, pageSize }`，由 MyBatis-Plus 分页插件输出。

## 数据库

建库脚本为 v1.2（6 张表，含演示数据），先建库再导入：

```sql
CREATE DATABASE footprint_planet DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Windows 命令行导入建议显式指定字符集，避免中文乱码：

```bash
mysql -uroot -p --default-character-set=utf8mb4 footprint_planet < footprint_planet.sql
```

| 表 | 作用 |
| --- | --- |
| `user` | 用户（`role`: ADMIN/USER；`status`: 0 正常 / 1 禁用） |
| `visited_city` | 点亮过的城市，`records` 存旅行回忆 |
| `plan` | 旅游规划（`days` / `waypoints` 为 JSON） |
| `note` | 社区笔记（`photos` 为 JSON，冗余 `author_nickname` / `location_*` 便于列表展示） |
| `note_like` | 笔记点赞关系 |
| `feedback` | 建议反馈（`handled`: 处理状态） |

**演示账号**（密码均为 `123456`）：`admin`（管理员，角色 ADMIN）、`xiaoA`、`zhangsan`、`zhuiguangzhe`、`xinghe`（普通用户，角色 USER）。

## 本地启动

### 1. 准备

- JDK 17、Maven 3.9+；
- MySQL 已建好 `footprint_planet` 库并导入上述脚本；
- 图片上传依赖阿里云 OSS，未配置密钥时上传接口不可用（不影响登录等其它功能）。

### 2. 配置账号密码（关键）

所有环境相关敏感配置集中在 `src/main/resources/application-dev.yml`，请填写：

```yaml
footprint:
  datasource:
    username: root            # 本地 MySQL 账号
    password: 123456          # 本地 MySQL 密码
  redis:
    host: localhost           # 无 Redis 则留默认即可，服务不依赖它启动
    port: 6379
aliyun:
  oss:
    access-key-id: xxx        # OSS 密钥（占位，未开通则保持占位）
    access-key-secret: xxx
    bucket-name: xxx
    url-prefix: https://xxx.oss-cn-hangzhou.aliyuncs.com
```

- `application.yml` 默认激活 `dev` profile，部署新环境时可复制 `application-dev.yml` 为 `application-{env}.yml` 再切换。
- `application.yml` 中 `footprint.jwt.secret / expiration` 为 JWT 签名密钥与有效期，生产部署请更换。

### 3. 运行

```bash
mvn spring-boot:run
# 或 IDE 中直接运行 com.qzxx.footprint_planet.FootprintPlanetApplication
```

启动成功后服务监听 `http://localhost:8080`。

### 4. 验证

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
# => {"code":0,"message":"ok","data":{...token...}}
```

## 关联资源

- 前端仓库：`footprint-planet`（Vue 3 + Vite + Pinia，Vite dev 代理 `/api` → `localhost:8080`）
- 接口文档：《足迹星球后端接口文档》(v1.2，33 接口)
- 建库脚本：`footprint_planet.sql`（v1.2）

## License

本项目仅供学习交流使用。
