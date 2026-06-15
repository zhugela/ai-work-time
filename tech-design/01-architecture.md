# 个人记账后端 · 系统架构与技术选型（01-architecture.md）

> 配套文档：[PRD.md](../docs/prd/PRD.md) / [AC.md](../docs/prd/AC.md) / [03-db-schema.sql](./03-db-schema.sql) / [04-api-spec.yaml](./04-api-spec.yaml)
> 版本 v1.0 · 2026-06-15

---

## 1. 总体架构

### 1.1 部署形态
```
┌──────────────────────────────────────────────────────────────┐
│                    用户终端（Web / 小程序 / APP）              │
└──────────────────────────┬───────────────────────────────────┘
                           │ HTTPS · Bearer Token
                           ▼
┌──────────────────────────────────────────────────────────────┐
│             Nginx 反向代理（TLS 终结 + 静态资源）             │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│         SpringBoot 单体应用 (Java 17, port 8080)             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │  Controller │  │ Service │  │  Domain  │  │   AOP    │    │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘    │
│       └──────────────┴─────────────┴─────────────┘            │
│                       │ Flyway │ JPA │ POI │ JJWT            │
└──────────────────────────┬───────────────────────────────────┘
                           │ JDBC
                           ▼
┌──────────────────────────────────────────────────────────────┐
│                   MySQL 8.0  (3306)                            │
│                   Redis 7.x (6379, 缓存/限流)                 │
└──────────────────────────────────────────────────────────────┘
```

### 1.2 模式选择
- **架构**：SpringBoot **单体**（非微服务）
- **协作模式**：前后端**完全分离**（BFF 仅一份 API 文档，OpenAPI 3.0）
- **运行方式**：单 jar 部署；通过 `application-{profile}.yml` 切换环境
- **升级路径**：单体 → 拆分模块（pkg by feature）→ 二期可演进为模块化单体（Spring Modulith）

---

## 2. 技术选型清单

### 2.1 核心框架
| 组件 | 版本 | 选型理由 |
|---|---|---|
| **JDK** | 17 LTS | SpringBoot 3.x 基线；Records/Sealed 提升代码可读性；性能优于 JDK 11 ~10% |
| **SpringBoot** | 3.2.5 | 当前 GA 稳定版；原生支持虚拟线程（`spring.threads.virtual.enabled=true`） |
| **构建** | Maven 3.9.x | 团队熟悉度高于 Gradle；多模块扩展方便 |
| **Maven 仓库** | 阿里云 + Maven Central | 国内拉取速度 + 中央源回退 |

### 2.2 Web / 校验
| 组件 | 版本 | 选型理由 |
|---|---|---|
| **spring-boot-starter-web** | 3.2.5 | 内置 Tomcat（默认）；支持虚拟线程 |
| **spring-boot-starter-validation** | 3.2.5 | Hibernate Validator；JSR-380 标准 |
| **spring-boot-starter-actuator** | 3.2.5 | 健康检查、Prometheus 指标 |

### 2.3 安全
| 组件 | 版本 | 选型理由 |
|---|---|---|
| **spring-boot-starter-security** | 3.2.5 | 与 Spring 生态深度集成；FilterChain 灵活 |
| **jjwt-api/impl/jackson** | 0.12.5 | 业内标准 JWT 库；无外部依赖；API/Impl 分离避免引入传递依赖 |
| **BCrypt** | 内置 | cost=10（百毫秒级）；无需额外依赖 |

### 2.4 持久层
| 组件 | 版本 | 选型理由 |
|---|---|---|
| **spring-boot-starter-data-jpa** | 3.2.5 | 减少 SQL 模板代码；Specification 支持动态查询 |
| **HikariCP** | 内置 | SpringBoot 默认；性能与稳定性业界第一 |
| **MySQL Connector/J** | 8.3.0 | 官方驱动；支持新鉴权插件 |
| **Flyway** | 9.22.x | 版本化迁移；CI/CD 友好；社区版免费 |
| **QueryDSL** | 5.0.0 | 可选；复杂统计查询拼装（与 JPA Specification 二选一） |

### 2.5 工具库
| 组件 | 版本 | 选型理由 |
|---|---|---|
| **Lombok** | 1.18.30 | 减少 getter/setter 噪音；与 SpringBoot 兼容良好 |
| **MapStruct** | 1.5.5 | 编译期生成映射代码，比反射快 10x |
| **Apache Commons Lang3** | 3.14.0 | 字符串/集合/日期工具 |
| **Hutool** | 5.8.x | 国内常用；可选（避免过度依赖第三方） |
| **Guava** | 32.1.x | 不可变集合/限流/LoadingCache |
| **Caffeine** | 3.1.8 | 本地缓存（如未引入 Redis 时的兜底） |

### 2.6 Excel
| 组件 | 版本 | 选型理由 |
|---|---|---|
| **Apache POI (poi-ooxml)** | 5.2.5 | xlsx 读写；SXSSF 流式导出避免 OOM |
| **EasyExcel** | 3.3.x | 国内阿里出品；导入 API 友好（备选） |

> **决定**：用 POI，理由：导出与导入同库；避免 EasyExcel 维护活跃度风险

### 2.7 接口文档
| 组件 | 版本 | 选型理由 |
|---|---|---|
| **springdoc-openapi-starter-webmvc-ui** | 2.5.0 | SpringBoot 3 原生；自动从 Controller 生成；Swagger UI 内置 |
| **Knife4j** | 4.5.x | 国内增强（搜索/导出/调试）；选配增强体验 |

### 2.8 监控与可观测
| 组件 | 版本 | 选型理由 |
|---|---|---|
| **Micrometer Prometheus** | 内置 | Actuator 指标导出 |
| **Logback** | 内置 | 日志框架 |
| **Logstash Logback Encoder** | 7.4 | JSON 格式日志便于 ELK 收集 |
| **MDC + TraceId** | — | 跨服务追踪预留 |

### 2.9 测试
| 组件 | 版本 | 选型理由 |
|---|---|---|
| **JUnit 5** | 5.10.x | 与 SpringBoot 3 配套 |
| **Mockito** | 5.x | 单元测试 |
| **Testcontainers** | 1.19.x | 真实 MySQL 容器测集成 |
| **Spring Boot Test** | 内置 | `@WebMvcTest` / `@DataJpaTest` |
| **REST Assured** | 5.4.x | API 集成测试 |
| **Playwright Java** | 1.42.x | 端到端冒烟（参照 prototype/） |

### 2.10 部署 / DevOps
| 组件 | 版本 | 选型理由 |
|---|---|---|
| **Docker** | 24+ | 标准化镜像 |
| **docker-compose** | v2 | 单机编排；本地/MVP 部署 |
| **Alpine JDK 17** | — | 镜像约 200MB |
| **Nginx** | 1.24 | 反向代理 + TLS 终结 |
| **GitHub Actions / GitLab CI** | — | CI 流水线 |
| **Maven Wrapper** | 3.9.x | 锁定 Maven 版本 |

### 2.11 缓存 / 限流
| 组件 | 版本 | 选型理由 |
|---|---|---|
| **Redis** | 7.2 | 可选；用于幂等键、限流计数、缓存 |
| **Bucket4j** | 8.x | 令牌桶限流（基于 Redis 分布式） |
| **Caffeine** | 3.1.8 | 单机兜底缓存（无 Redis 时） |

### 2.12 前端选型（基于 prototype/）
| 组件 | 版本 | 选型理由 |
|---|---|---|
| **Vue 3** | 3.4 | 学习曲线平缓；中文社区活跃 |
| **Vite** | 5.x | 极速冷启动 |
| **Element Plus** | 2.7 | 中后台组件库；与 prototype 视觉一致 |
| **Pinia** | 2.x | 状态管理 |
| **Vue Router** | 4.x | 路由 |
| **Axios** | 1.7 | HTTP 客户端（拦截器统一处理 Token/错误码） |
| **ECharts** | 5.5 | 趋势/占比图（若 G-07 决议用三方库） |
| **xlsx** | 0.18 | 浏览器侧 Excel 解析（如需前端预览） |

> **决定**：前端栈保留弹性，本期主交付后端，前端可由甲方按本规范自行实现

### 2.13 可选 / 弃用
| 选项 | 状态 | 原因 |
|---|---|---|
| Spring Cloud Gateway | ❌ 不用 | 单体不需要 |
| Spring Security OAuth2 | ❌ 不用 | 本期无第三方登录（PRD Scope） |
| Dubbo / gRPC | ❌ 不用 | 单体内部调用直接走 Spring Bean |
| Kotlin | ❌ 不用 | 团队 Java 背景 |
| WebFlux | ❌ 不用 | 数据驱动型 CRUD，MVC 性能已足够 |

---

## 3. 模块划分

```
com.personal.jz
├── JzApplication.java
├── common
│   ├── api            # ApiResponse, PageResponse
│   ├── audit          # @AuditLog + AOP
│   ├── exception      # BizException, ErrorCode, GlobalExceptionHandler
│   ├── idempotency    # 幂等键
│   ├── ratelimit      # 限流
│   ├── security       # CurrentUser, CurrentUserResolver
│   └── validation     # 自定义注解
├── config
│   ├── OpenApiConfig
│   ├── SecurityConfig
│   └── WebConfig
├── security
│   ├── JwtTokenProvider
│   ├── JwtAuthenticationFilter
│   └── UserDetailsServiceImpl
├── module
│   ├── auth           # 登录注册改密
│   ├── book           # 账本
│   ├── category       # 分类
│   ├── tx             # 收支
│   ├── budget         # 预算
│   ├── stats          # 统计
│   ├── imexport       # 导入导出
│   └── admin          # 管理员
├── entity             # JPA 实体
├── repository         # JPA Repository
└── util
```

**包规范**（pkg by feature）：
- 每个 module 内部：`controller / service / dto / entity / repository / enums`
- 跨模块公共代码入 `common/`

---

## 4. 接口分层约定

| 层 | 职责 | 禁止事项 |
|---|---|---|
| Controller | 参数校验 / 调 service / 包装响应 | 不写业务逻辑 |
| Service | 业务逻辑 / 事务 / 异常 | 不直接接触 HTTP |
| Repository | 数据访问 | 不写业务规则 |
| Entity | 映射数据库 | 不含业务方法 |
| DTO | 跨层数据传输 | 不暴露 password_hash 等敏感字段 |

---

## 5. 关键流程

### 5.1 请求生命周期
```
HTTP Request
  → Nginx (TLS + 静态)
  → SpringBoot
     → CorsFilter
     → RateLimitFilter
     → JwtAuthenticationFilter
     → SecurityContext
     → @CurrentUser 注入
     → @Valid 校验
     → Controller
     → Service (@Transactional)
     → Repository (JPA)
     → MySQL/Redis
  → Response (ApiResponse<T>)
```

### 5.2 异常流转
```
业务异常 BizException(code)
  → GlobalExceptionHandler
  → ApiResponse {code, msg, data:null, traceId}

校验异常 MethodArgumentNotValid
  → GlobalExceptionHandler
  → ApiResponse {code: PARAM_INVALID, msg, data: fieldErrors[]}

未认证 AuthenticationException
  → AuthenticationEntryPoint
  → ApiResponse {code: TOKEN_*}

越权 AccessDeniedException
  → AccessDeniedHandler
  → ApiResponse {code: FORBIDDEN}
```

---

## 6. 部署方案

### 6.1 镜像构建
- 多阶段 build：builder（maven:3.9-eclipse-temurin-17）+ runtime（eclipse-temurin:17-jre-alpine）
- 镜像大小目标 < 250MB
- 镜像 tag 策略：`{version}-{git short sha}`（如 `1.0.0-a1b2c3d`）

### 6.2 编排
- `docker-compose.yml` 包含：`mysql:8.0` + `app` + `nginx` + `redis:7-alpine`
- 健康检查：`actuator/health/liveness` + `actuator/health/readiness`
- 数据卷：`mysql-data` 命名卷持久化

### 6.3 配置外置
- 敏感信息（DB 密码、JJWT 密钥）通过环境变量注入
- 配置文件：`application.yml` + `application-{profile}.yml`
- 配置中心（可选二期）：Nacos / Consul

### 6.4 上线 Checklist
- [ ] Flyway 迁移脚本已 dry-run
- [ ] 单元测试覆盖率 ≥ 70%
- [ ] Postman 冒烟 100% PASS
- [ ] k6 压测报告归档
- [ ] Prometheus 指标已接入
- [ ] ELK 日志通道打通
- [ ] 备份策略（mysqldump 每日）
- [ ] TLS 证书就绪

---

## 7. 异常容错策略

| 故障 | 应对 |
|---|---|
| MySQL 短暂不可用 | HikariCP 重试 + Spring Retry（指数退避） |
| Redis 不可用 | Caffeine 本地缓存兜底；限流降级为单机版 |
| 导入大批量数据 | 异步队列（`@Async` + 线程池）+ 进度查询 |
| 导出超时 | SXSSF 流式 + 浏览器侧 `Content-Disposition: attachment` |
| 分类删除冲突 | 业务级先迁移记录再删（提示用户） |
| Excel 公式注入 | 校验 `cell.getCellType() == FORMULA` 直接拒绝 |
| JWT 密钥泄漏 | 紧急吊销：清空 `auth_token` 表所有记录 |
| 高并发写入 | 行级悲观锁（`SELECT ... FOR UPDATE` 仅在余额敏感场景） |

---

## 8. 安全设计

| 维度 | 措施 |
|---|---|
| **传输** | 全站 HTTPS；HSTS |
| **认证** | JWT（HS256）+ 7 天有效 + 主动吊销 |
| **鉴权** | URL 级 + 方法级（`@PreAuthorize`） |
| **密码** | BCrypt(cost=10)；登录失败 5 次锁 30 分钟 |
| **SQL 注入** | JPA 参数化；禁止拼接 SQL |
| **XSS** | 输出转义；CSP 头 |
| **CSRF** | 前后端分离 + Token 不放 Cookie；天然免疫 |
| **越权** | 每个 service 方法首行校验 `resource.userId == current.id` |
| **审计** | 关键操作写 `audit_log`（180 天保留） |
| **敏感字段** | `password_hash` `@JsonIgnore`；日志脱敏 |
| **限流** | Bucket4j 令牌桶；登录 5/min/IP，普通 60/min/用户 |
| **依赖扫描** | CI 集成 `dependency-check`（OWASP） |
| **密钥管理** | 密钥不进 Git；通过环境变量 / 密钥管理服务 |

---

## 9. 未来扩展预留

| 预留点 | 触发条件 | 二期方向 |
|---|---|---|
| **多用户共享账本** | PRD 投票通过 | 增加 `book_member` 表 + 角色枚举 |
| **第三方登录** | 用户量 > 5K | 引入 Spring Authorization Server |
| **多币种** | 海外用户出现 | `currency` 字段已存在；补汇率服务 |
| **微信账单同步** | 评分 > 80 分 | 增加 `external_sync` 模块 + 定时任务 |
| **AI 自动分类** | 训练数据积累 | 异步 ML 服务 + 反馈闭环 |
| **移动端 APP** | Web 流量 70% | Flutter / RN 复用 OpenAPI 文档 |
| **审计日志归档** | 单表 > 1M | ClickHouse / OSS 冷存储 |
| **灰度发布** | 团队 ≥ 5 人 | Spring Cloud LoadBalancer + 灰度标签 |
| **GraphQL** | 前端定制需求 | 引入 `spring-graphql` |
| **消息通知** | 用户要求 | 增加 `notification` 表 + WebSocket 推送 |

---

## 10. 选型决策记录（ADR 摘要）

| 决策 | 选项 | 选定 | 理由 |
|---|---|---|---|
| 架构 | 单体 vs 微服务 | **单体** | MVP 阶段用户量 < 10K；微服务运维成本不划算 |
| ORM | JPA vs MyBatis | **JPA** | CRUD 占比 80%；动态查询 Specification 足够 |
| 数据库 | MySQL vs PostgreSQL | **MySQL 8** | 团队熟悉度 + 运维工具链成熟 |
| 鉴权 | JWT vs Session | **JWT** | 前后端分离；服务端无状态 |
| 缓存 | Redis vs Caffeine | **Redis 优先 / Caffeine 兜底** | 二期微服务时可平滑升级 |
| Excel | POI vs EasyExcel | **POI** | 导出与导入共用 |
| 接口文档 | Swagger UI | **springdoc** | 与 SpringBoot 3 配套 |
| 限流 | Bucket4j vs Sentinel | **Bucket4j** | 轻量；Redis 模式成熟 |
| 前端 | Vue vs React | **保留弹性** | 本期主后端；前端栈由甲方决定 |

