# 个人记账后端 · 技术开发任务清单（02-tasks.md）

> 与 [PRD.md](../docs/prd/PRD.md) / [AC.md](../docs/prd/AC.md) / [01-architecture.md](./01-architecture.md) 配套
> 任务粒度：2~5 小时可自测；标注文件路径、分步、验证、依赖

---

## 任务依赖图

```
M0 脚手架 ──► M1 数据层 ──► M2 公共层 ──► M3 账号域
                                  │
                                  ├─► M4 账本域
                                  ├─► M5 分类域
                                  ├─► M6 收支域
                                  ├─► M7 预算域
                                  ├─► M8 统计域
                                  ├─► M9 导入导出域
                                  └─► M10 管理员域
                                  │
                                  ▼
M11 性能/安全加固 ──► M12 联调/冒烟 ──► M13 部署交付
```

---

## M0 · 工程脚手架

### T-01 初始化 SpringBoot 项目
**文件**：
- 创建：`backend/pom.xml`
- 创建：`backend/src/main/resources/application.yml`
- 创建：`backend/src/main/java/com/personal/jz/JzApplication.java`
- 测试：`backend/src/test/java/com/personal/jz/JzApplicationTests.java`

**Step 1**：Maven 配置 SpringBoot 3.2.x、Java 17、依赖 spring-boot-starter-web/security/validation/data-jpa、mysql-connector-j、flyway-mysql、jjwt、poi-ooxml、mapstruct、lombok、springdoc-openapi-starter-webmvc-ui、testcontainers
**Step 2**：编写 `application.yml` 包含 dev/prod 两套 profile
**Step 3**：`@SpringBootApplication` 启动类 + `applicationTests.contextLoads()` 通过
**验证**：`mvn -q test` exit 0；`mvn spring-boot:run` 启动 8080

### T-02 引入代码规范工具
**文件**：
- 创建：`backend/pom.xml`（plugin 段）
- 创建：`backend/.editorconfig`、`backend/checkstyle.xml`
- 创建：`backend/spotless/eclipse-formatter.xml`

**Step 1**：集成 Spotless + Checkstyle + Google Java Format
**Step 2**：CI 必须 `mvn verify` 通过
**验证**：`mvn spotless:check` exit 0

### T-03 多环境配置
**文件**：
- 创建：`backend/src/main/resources/application-dev.yml`
- 创建：`backend/src/main/resources/application-prod.yml`
- 创建：`backend/src/main/resources/logback-spring.xml`

**Step 1**：dev 用 H2，prod 用 MySQL 8
**Step 2**：日志按天滚动 + ERROR 触发企业微信 Webhook
**验证**：`--spring.profiles.active=dev` 启动 OK

### T-04 异常体系
**文件**：
- 创建：`backend/src/main/java/com/personal/jz/common/exception/BizException.java`
- 创建：`backend/src/main/java/com/personal/jz/common/exception/ErrorCode.java`
- 创建：`backend/src/main/java/com/personal/jz/common/exception/GlobalExceptionHandler.java`
- 创建：`backend/src/main/java/com/personal/jz/common/api/ApiResponse.java`
- 测试：`backend/src/test/java/com/personal/jz/common/exception/GlobalExceptionHandlerTest.java`

**Step 1**：定义 `ErrorCode` 枚举（≥40 项：账号/账本/分类/收支/预算/导入）
**Step 2**：`GlobalExceptionHandler` 捕获 BizException / MethodArgumentNotValid / ConstraintViolation / AccessDenied
**Step 3**：统一 `ApiResponse<T>{code, msg, data, traceId}`
**验证**：`MockMvc` 测试 4 类异常均返回正确包装

---

## M1 · 数据层

### T-05 Flyway 数据库初始化
**文件**：
- 创建：`backend/src/main/resources/db/migration/V1__init.sql`

**Step 1**：依据 [01-architecture.md](./01-architecture.md) §3 写入 8 张表 DDL
**Step 2**：`spring.flyway.enabled=true`
**验证**：`mvn spring-boot:run` 自动建表成功

### T-06 JPA 实体
**文件**：
- 创建：`backend/src/main/java/com/personal/jz/entity/{SysUser,Book,Category,TxRecord,Budget,AuthToken,AuditLog}.java`

**Step 1**：`@Entity` + Lombok `@Getter/Builder`；`@Table(name=...)`
**Step 2**：枚举字段 `@Enumerated(EnumType.STRING)`
**Step 3**：`SysUser.passwordHash` 标记 `@JsonIgnore`
**验证**：`mvn compile` 0 警告

### T-07 Repository
**文件**：
- 创建：`backend/src/main/java/com/personal/jz/repository/*.java`（7 个）

**Step 1**：继承 `JpaRepository<T,Long>` + `JpaSpecificationExecutor<T>`（支持分页+多维筛选）
**Step 2**：自定义方法（`findByUsernameAndEnabledTrue` 等）
**验证**：`@DataJpaTest` 跑通

### T-08 数据库索引优化
**文件**：
- 创建：`backend/src/main/resources/db/migration/V2__add_indexes.sql`

**Step 1**：补充 `tx_record` 复合索引 `(book_id, occurred_at DESC)`、`(user_id, occurred_at DESC)`、`(book_id, category_id)`
**Step 2**：`audit_log` 索引 `(user_id, created_at DESC)`
**验证**：`EXPLAIN` 关键查询走索引

---

## M2 · 公共层

### T-09 统一响应 + 分页包装
**文件**：
- 创建：`backend/src/main/java/com/personal/jz/common/api/ApiResponse.java`
- 创建：`backend/src/main/java/com/personal/jz/common/api/PageResponse.java`

**Step 1**：`ApiResponse<T>` 含 code/msg/data/traceId
**Step 2**：`PageResponse<T>` 含 list/page/size/total/hasNext
**验证**：`@WebMvcTest` 返回 JSON 结构

### T-10 JWT 工具
**文件**：
- 创建：`backend/src/main/java/com/personal/jz/security/JwtTokenProvider.java`
- 创建：`backend/src/main/java/com/personal/jz/security/JwtAuthenticationFilter.java`
- 测试：`backend/src/test/java/com/personal/jz/security/JwtTokenProviderTest.java`

**Step 1**：HS256 签名，claims 包含 uid/role/iat/exp
**Step 2**：`JwtAuthenticationFilter` OncePerRequestFilter
**验证**：单元测试签发+解析+过期

### T-11 Spring Security 配置
**文件**：
- 创建：`backend/src/main/java/com/personal/jz/security/SecurityConfig.java`
- 创建：`backend/src/main/java/com/personal/jz/security/UserDetailsServiceImpl.java`

**Step 1**：白名单 `/api/auth/**`、Swagger `/v3/api-docs/**`、Actuator
**Step 2**：自定义 `AccessDeniedHandler` + `AuthenticationEntryPoint`
**Step 3**：密码 `BCryptPasswordEncoder(strength=10)`
**验证**：未授权 401、权限不足 403

### T-12 操作审计 AOP
**文件**：
- 创建：`backend/src/main/java/com/personal/jz/common/audit/AuditLog.java`（注解）
- 创建：`backend/src/main/java/com/personal/jz/common/audit/AuditAspect.java`

**Step 1**：`@AuditLog("删除账本")` 注解
**Step 2**：切面记录 userId/action/targetId/ip/ua/result
**Step 3**：异常分支也要写
**验证**：单测覆盖正常+异常

### T-13 当前用户上下文
**文件**：
- 创建：`backend/src/main/java/com/personal/jz/common/security/CurrentUser.java`
- 创建：`backend/src/main/java/com/personal/jz/common/security/CurrentUserResolver.java`

**Step 1**：`@CurrentUser UserPrincipal`
**Step 2**：在 `HandlerMethodArgumentResolver` 注入
**验证**：受保护接口可获取

### T-14 通用校验器
**文件**：
- 创建：`backend/src/main/java/com/personal/jz/common/validation/*.java`

**Step 1**：自定义 `@Password`、`@Username`、`@MonthFormat` 等注解
**Step 2**：`ConstraintValidator` 实现
**验证**：Bean Validation 单测

### T-15 跨域 CORS
**文件**：
- 修改：`backend/src/main/java/com/personal/jz/security/SecurityConfig.java`

**Step 1**：`CorsConfigurationSource` 配置 allowedOrigins/Methods/Headers
**Step 2**：仅 dev 环境开启 `*`；prod 走白名单
**验证**：`OPTIONS` 预检通过

### T-16 幂等性键
**文件**：
- 创建：`backend/src/main/java/com/personal/jz/common/idempotency/IdempotencyAspect.java`

**Step 1**：`Idempotency-Key` 头 24h 内同 key 同响应
**Step 2**：Redis 存响应体（如未引入 Redis，则内存 ConcurrentHashMap 兜底）
**验证**：相同 key 第二次返回缓存

### T-17 OpenAPI/Swagger UI
**文件**：
- 创建：`backend/src/main/java/com/personal/jz/config/OpenApiConfig.java`

**Step 1**：`springdoc-openapi-starter-webmvc-ui`
**Step 2**：`/swagger-ui.html` 可访问
**Step 3**：JWT Bearer 鉴权可视化
**验证**：所有 Controller 注解 `@Operation/@Tag`

---

## M3 · 账号域

### T-18 注册接口
**文件**：
- 创建：`backend/src/main/java/com/personal/jz/module/auth/dto/RegisterRequest.java`
- 创建：`backend/src/main/java/com/personal/jz/module/auth/service/AuthService.java`
- 创建：`backend/src/main/java/com/personal/jz/module/auth/controller/AuthController.java`
- 测试：`backend/src/test/java/com/personal/jz/module/auth/AuthServiceTest.java`

**Step 1**：参数校验 `@Valid`
**Step 2**：BCrypt 编码；唯一索引冲突 → `USERNAME_TAKEN`
**Step 3**：成功后自动创建「日常账本」+ 系统预置分类
**Step 4**：返回 token
**验证**：单测覆盖弱密码/已存在/正常

### T-19 登录接口
**文件**：
- 创建：`backend/src/main/java/com/personal/jz/module/auth/dto/LoginRequest.java`
- 创建：`backend/src/main/java/com/personal/jz/module/auth/dto/LoginResponse.java`
- 修改：`AuthService.java`
- 测试：`AuthServiceTest.java`

**Step 1**：`failedLoginCount` 自增；≥5 锁 30 分钟
**Step 2**：签发 JWT + 写 `auth_token`
**Step 3**：返回用户基本信息（脱敏）
**验证**：5 次失败后第 6 次返回 423

### T-20 改密 / 改昵称
**文件**：
- 修改：`AuthService.java`
- 创建：`ChangePasswordRequest.java`、`UpdateProfileRequest.java`

**Step 1**：原密码错 → `WRONG_OLD_PASSWORD`
**Step 2**：新密码强度校验 + 二次密码一致
**Step 3**：成功后将该用户所有 `auth_token.revoked=1`
**验证**：改密后旧 token 失效

### T-21 退出 / 当前用户
**Step 1**：`POST /api/auth/logout` 把当前 token revoked
**Step 2**：`GET /api/auth/me` 返回当前用户
**验证**：退出后该 token 访问其他接口 401

### T-22 登录失败锁定单测
**验证**：`failed_login_count` 边界值测试

---

## M4 · 账本域

### T-23 Book Entity + Repository
**Step 1**：复合唯一键 `(user_id, name)` 在 Repository 层校验
**验证**：同名重复 → `BOOK_NAME_TAKEN`

### T-24 账本 Service
**文件**：
- 创建：`backend/src/main/java/com/personal/jz/module/book/BookService.java`
- 创建：`BookController.java`
- 创建：`CreateBookRequest.java`、`UpdateBookStatusRequest.java`

**Step 1**：创建账本 → 复制系统预置分类（type=INCOME/EXPENSE 全量）
**Step 2**：归档最后一个活跃账本 → `LAST_ACTIVE_BOOK`
**Step 3**：归档账本下不可写
**验证**：单测覆盖边界

### T-25 切换账本
**Step 1**：`SysUser.current_book_id` 更新
**Step 2**：目标账本必须 ACTIVE 且属于当前用户
**验证**：切换后所有列表/统计走新账本

---

## M5 · 分类域

### T-26 系统预置分类初始化
**文件**：
- 创建：`backend/src/main/java/com/personal/jz/module/category/CategoryBootstrap.java`
- 创建：`backend/src/main/resources/db/migration/V3__seed_categories.sql`

**Step 1**：启动时幂等写入 15 支出 + 8 收入
**Step 2**：`is_system=1`
**验证**：重复启动不抛异常

### T-27 分类 CRUD
**Step 1**：`POST/PUT/DELETE /api/categories`
**Step 2**：类型枚举校验
**Step 3**：删除受 `tx_record` RESTRICT 保护
**验证**：被引用的分类不能删

---

## M6 · 收支域

### T-28 录入接口
**文件**：
- 创建：`backend/src/main/java/com/personal/jz/module/tx/TxService.java`
- 创建：`TxController.java`
- 创建：`CreateTxRequest.java`

**Step 1**：分类 type 一致性校验
**Step 2**：金额 `> 0 && ≤ 9,999,999.99`
**Step 3**：日期 ≤ today+7
**Step 4**：写入后触发预算检查
**验证**：分类类型错 → 400

### T-29 收支列表（多维筛选）
**Step 1**：`Specification` 动态拼查询
**Step 2**：`Pageable` 默认 `PageRequest.of(0, 20, Sort.by("occurredAt").descending())`
**验证**：10 万条数据查询 < 500ms

### T-30 改/删/详情
**Step 1**：删除 `is_deleted=1` 软删除
**Step 2**：归档账本下记录只读
**验证**：操作他人记录 → 404

### T-31 预算超支检查
**文件**：
- 创建：`backend/src/main/java/com/personal/jz/module/budget/BudgetService.java`

**Step 1**：录入响应携带 `overBudgetWarning: boolean`
**Step 2**：阈值从 `budget.warn_threshold` 读取
**Step 3**：超阈值不抛异常，仅写响应字段
**验证**：mock 预算已用 102% → 响应携带 true

---

## M7 · 预算域

### T-32 设置/更新预算
**Step 1**：`PUT /api/budgets`；同 book+category+month upsert
**Step 2**：INCOME 分类 → `BUDGET_ON_INCOME`
**验证**：金额=0 → 400

### T-33 预算进度
**Step 1**：聚合 `tx_record` 当月支出（按 category_id 汇总）
**Step 2**：返回 `{budget, used, progress, status}`
**Step 3**：未设置 → `progress=null`
**验证**：空月份进度为 null

---

## M8 · 统计域

### T-34 月度汇总
**Step 1**：`SUM(CASE WHEN type='INCOME' THEN amount ELSE 0 END)` 等
**验证**：与明细求和一致

### T-35 分类占比
**Step 1**：按 `category_id` 聚合支出
**Step 2**：排序取前 10 + 其他
**Step 3**：占比合计校验
**验证**：100.00% 误差 ≤ 0.01

### T-36 趋势
**Step 1**：按月聚合；空月份补 0
**Step 2**：6/12 月枚举
**验证**：跨年查询无重复

---

## M9 · 导入导出域

### T-37 Excel 导出
**文件**：
- 创建：`backend/src/main/java/com/personal/jz/module/imexport/ExportService.java`
- 创建：`backend/src/main/java/com/personal/jz/module/imexport/ExcelWriter.java`

**Step 1**：明细导出使用 Apache POI SXSSF
**Step 2**：月度多 Sheet
**Step 3**：金额用字符串避免精度
**验证**：生成 xlsx 可用 WPS 打开

### T-38 Excel 导入
**文件**：
- 创建：`ImportService.java`
- 创建：`ExcelParser.java`

**Step 1**：表头校验
**Step 2**：分类按名称查找当前账本；找不到 → 行失败
**Step 3**：检测 `=` 开头 → `FORMULA_INJECTION`
**Step 4**：原子写；失败行不入库
**Step 5**：返回 `{success, failed[{row,col,msg}]}`
**验证**：故意混入 8 类错误，全部精准归类

### T-39 导入模板
**Step 1**：固定表头 + 1 行示例
**验证**：用户按模板填入可成功导入

---

## M10 · 管理员域

### T-40 ADMIN 角色 + 鉴权
**Step 1**：`@PreAuthorize("hasRole('ADMIN')")`
**Step 2**：默认 ADMIN 通过 `application.yml` 初始化
**验证**：普通用户访问 `/api/admin/**` → 403

### T-41 启停用户
**Step 1**：`enabled=false` → 该用户所有 token revoked
**Step 2**：不能停用自己
**验证**：自己停用 → 400

### T-42 平台规模统计
**Step 1**：聚合 SQL
**Step 2**：分页 + 时间范围
**验证**：10 万用户 < 1s

### T-43 审计日志查询
**Step 1**：`/api/admin/audit-logs?userId=&action=&from=&to=&page=`
**Step 2**：保留 180 天定时清理
**验证**：过期日志自动归档/删除

---

## M11 · 性能/安全加固

### T-44 限流
**文件**：
- 创建：`RateLimitFilter.java`
- 创建：`RateLimiter.java`（令牌桶）

**Step 1**：登录 5 次/分钟/IP
**Step 2**：普通接口 60 次/分钟/用户
**验证**：超限返回 429

### T-45 SQL 注入 + XSS
**Step 1**：MyBatis/JPA 全部参数化
**Step 2**：搜索字段长度限制 + 输出转义
**验证**：扫描器无高危

### T-46 缓存层（可选）
**Step 1**：分类列表走 Redis（如未引入则用 Caffeine）
**Step 2**：TTL 5 min
**验证**：命中后 P95 < 50ms

### T-47 日志脱敏
**Step 1**：password/token/email 在日志中 `***`
**Step 2**：logback MDC 注入 traceId
**验证**：抓包无明文密码

---

## M12 · 联调与冒烟

### T-48 Newman 接口冒烟
**文件**：
- 创建：`backend/postman/JiZhang.postman_collection.json`

**Step 1**：导入全部 Must/Should 接口
**Step 2**：`newman run` 在 CI 必过
**验证**：100% 通过

### T-49 端到端用例
**Step 1**：Playwright Java 跑 12 页面（参照 prototype/）
**Step 2**：登录→建账本→录入→看统计
**验证**：E2E 流程 < 30s 通过

### T-50 性能压测
**Step 1**：`k6` 或 JMeter 100/500 并发
**Step 2**：P95 < 500ms；5xx < 0.1%
**验证**：压测报告归档

---

## M13 · 部署交付

### T-51 Dockerfile
**文件**：
- 创建：`backend/Dockerfile`（多阶段 build）
- 创建：`docker-compose.yml`（MySQL + App）

**Step 1**：JDK 17 基础镜像
**Step 2**：`docker-compose up` 一键启动
**验证**：服务连通 MySQL

### T-52 数据库迁移脚本
**文件**：
- 创建：`tech-design/03-db-schema.sql`（交付版）

**Step 1**：导出 Flyway 后状态
**Step 2**：提供空库 + 示例数据两套
**验证**：干净 MySQL 导入后应用启动 OK

### T-53 OpenAPI 文档
**文件**：
- 创建：`tech-design/04-api-spec.yaml`

**Step 1**：所有 Controller 注解齐全
**Step 2**：`/v3/api-docs.yaml` 导出
**验证**：通过 Swagger Editor 校验

### T-54 Release Notes
**文件**：
- 创建：`docs/release/RELEASE_NOTES_v1.0.md`

**Step 1**：功能清单 / 已知问题 / 升级路径
**验证**：上线 Checklist 全过

---

## 关键依赖关系

| 上游任务 | 下游任务 | 阻塞点 |
|---|---|---|
| T-01~04 | 全部 | 脚手架/异常/响应包装 |
| T-05~08 | T-18~43 | 数据层 |
| T-09~17 | T-18~43 | 公共层（鉴权、当前用户、AOP） |
| T-18~22 | T-28~30 | 账号必须先有才能录数据 |
| T-26 | T-28 | 录入前必须有预置分类 |
| T-31 | T-32 | 预算服务依赖预算检查服务 |
| T-37 | T-38 | 导入/导出共用 POI 工具 |
| T-48~50 | T-51 | 压测报告是上线必要条件 |

---

## 角色与 RACI（建议）

| 模块 | 主 R（执行） | 主 A（负责） | C（咨询） | I（知会） |
|---|---|---|---|---|
| M0~M2 | 后端 TL | 架构师 | DBA | PM |
| M3~M6 | 后端 A | 后端 TL | 安全 | PM |
| M7~M9 | 后端 B | 后端 TL | PM | QA |
| M10 | 后端 TL | 架构师 | 安全 | PM |
| M11 | 安全 | 架构师 | 后端 TL | PM |
| M12~M13 | QA + 运维 | 架构师 | PM | 全员 |
