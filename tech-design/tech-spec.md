# 个人记账后端 · 技术方案总文档（tech-spec.md）

> 项目编号 **PJ-2026-06-15-JIZHANG** · 版本 v1.0 · 2026-06-15
> 配套：[01-architecture.md](./01-architecture.md) · [02-tasks.md](./02-tasks.md) · [03-db-schema.sql](./03-db-schema.sql) · [04-api-spec.yaml](./04-api-spec.yaml) · [05-schedule.md](./05-schedule.md)

---

## 0. 文档目的

本文档作为整体技术方案的**总入口**，聚焦六大事项：
1. 系统架构图
2. 模块划分
3. 部署方案
4. 异常容错策略
5. 安全设计
6. 未来扩展预留设计

> 详细技术细节（选型、字段、接口、任务、排期）见配套 5 份子文档。

---

## 1. 系统架构图

### 1.1 物理拓扑
```
                  ┌────────────────────┐
                  │   用户终端 (Web/小程序) │
                  └──────────┬─────────┘
                             │ HTTPS · JWT
                             ▼
                  ┌────────────────────┐
                  │   Nginx (TLS+静态)  │   :443
                  └──────────┬─────────┘
                             │ Reverse Proxy
                             ▼
        ┌────────────────────────────────────────┐
        │  SpringBoot 单体应用 (Java 17)         │   :8080
        │  ├─ CorsFilter / RateLimit / JwtFilter │
        │  ├─ Controller (33 接口)               │
        │  ├─ Service (事务/业务)                │
        │  ├─ Repository (JPA)                   │
        │  ├─ AOP (审计/幂等)                    │
        │  └─ POI / Flyway / JJWT / Bucket4j     │
        └────────────────┬───────────────────────┘
                         │ JDBC
                ┌────────┴────────┐
                ▼                 ▼
       ┌──────────────┐   ┌──────────────┐
       │ MySQL 8.0    │   │ Redis 7.x    │
       │ 主库+只读副本 │   │ 缓存/限流/幂等│
       └──────────────┘   └──────────────┘
                 ▲
                 │ 每日 mysqldump → OSS 冷备
                 │
       ┌──────────┴───────────┐
       │ Prometheus + Grafana  │  ← 监控告警
       │ ELK 日志              │
       └──────────────────────┘
```

### 1.2 逻辑分层
```
┌──────────────────────────────────────┐
│ 接入层  Nginx / WAF / TLS            │
├──────────────────────────────────────┤
│ 过滤层  Cors · RateLimit · JwtAuth   │
├──────────────────────────────────────┤
│ 接入层  Controller (33 endpoints)    │
├──────────────────────────────────────┤
│ 业务层  Service (8 modules)          │
├──────────────────────────────────────┤
│ 横切层  Audit AOP · Idempotency AOP  │
├──────────────────────────────────────┤
│ 数据层  JPA Repository (7 repos)     │
├──────────────────────────────────────┤
│ 存储层  MySQL 8.0 / Redis 7.x        │
└──────────────────────────────────────┘
```

> 详见 [01-architecture.md §1](./01-architecture.md)

---

## 2. 模块划分

### 2.1 业务模块
| # | 模块 | 路径 | 关键类 |
|---|---|---|---|
| M3 | Auth 账号 | `module/auth` | AuthService, AuthController |
| M4 | Book 账本 | `module/book` | BookService, BookController |
| M5 | Category 分类 | `module/category` | CategoryService, CategoryBootstrap |
| M6 | Transaction 收支 | `module/tx` | TxService, TxController, BudgetCheckService |
| M7 | Budget 预算 | `module/budget` | BudgetService |
| M8 | Stats 统计 | `module/stats` | MonthlyStatsService, BreakdownService, TrendService |
| M9 | Import/Export | `module/imexport` | ExcelWriter, ExcelParser |
| M10 | Admin 管理员 | `module/admin` | UserAdminService, AuditQueryService |

### 2.2 横切关注点
| 模块 | 路径 | 说明 |
|---|---|---|
| Common-Exception | `common/exception` | BizException, ErrorCode, GlobalExceptionHandler |
| Common-Api | `common/api` | ApiResponse<T>, PageResponse<T> |
| Common-Security | `common/security` | @CurrentUser 注解 + 解析器 |
| Common-Audit | `common/audit` | @AuditLog 注解 + AOP |
| Common-Idempotency | `common/idempotency` | 幂等键 AOP |
| Common-RateLimit | `common/ratelimit` | 令牌桶限流 |
| Common-Validation | `common/validation` | 自定义 Bean Validation |

### 2.3 数据实体映射
```
sys_user ──┬─< book ──< category ──< tx_record
           │            └─< budget
           ├─< auth_token
           └─< audit_log
```

> 详见 [03-db-schema.sql](./03-db-schema.sql)

### 2.4 接口契约
33 个 REST 端点，分 8 个 tag：
- Auth: 6 个
- Book: 5 个
- Category: 4 个
- Transaction: 5 个
- Budget: 2 个
- Stats: 3 个
- ImportExport: 4 个
- Admin: 3 个
- Me（嵌入 Auth）: 1 个

> 详见 [04-api-spec.yaml](./04-api-spec.yaml)

---

## 3. 部署方案

### 3.1 部署环境矩阵
| 环境 | 用途 | 基础设施 | 数据库 | 域名 |
|---|---|---|---|---|
| dev | 开发联调 | 本机 | H2 内存库 | localhost |
| test | QA 测试 | 单机 docker | MySQL 8.0 (容器) | test.personal-jz.local |
| staging | 客户 UAT | K8s 1 节点 | MySQL 8.0 + Redis 7 | staging.personal-jz.local |
| prod | 生产 | K8s 3 节点 | MySQL 8.0 主从 + Redis Sentinel | api.personal-jz.com |

### 3.2 镜像构建
- 多阶段 Docker：`builder (maven:3.9-eclipse-temurin-17) + runtime (eclipse-temurin:17-jre-alpine)`
- 镜像大小目标 < 250MB
- Tag 策略：`{version}-{git short sha}` 例 `1.0.0-a1b2c3d`
- 健康检查：`actuator/health/liveness` + `actuator/health/readiness`

### 3.3 docker-compose（test/staging）
```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: personal_jz
    volumes:
      - mysql-data:/var/lib/mysql
      - ./tech-design/03-db-schema.sql:/docker-entrypoint-initdb.d/V1__init.sql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "127.0.0.1"]
      interval: 10s
      retries: 5

  redis:
    image: redis:7-alpine
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s

  app:
    build: ./backend
    depends_on:
      mysql: { condition: service_healthy }
      redis: { condition: service_healthy }
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/personal_jz
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      SPRING_REDIS_HOST: redis
      JJWT_SECRET: ${JJWT_SECRET}
    ports:
      - "8080:8080"

  nginx:
    image: nginx:1.24-alpine
    volumes:
      - ./deploy/nginx.conf:/etc/nginx/nginx.conf
    ports:
      - "443:443"
    depends_on: [app]

volumes:
  mysql-data:
```

### 3.4 私有化部署清单
- [ ] 客户环境满足 JDK 17 + Docker
- [ ] MySQL 8.0 已建库（执行 03-db-schema.sql）
- [ ] Redis 7.x 已部署
- [ ] TLS 证书就位
- [ ] 防火墙放通 443 / 8080（仅内网）
- [ ] 备份策略 mysqldump 每日 02:00

### 3.5 上线 Checklist
- [ ] 单元测试覆盖率 ≥ 70%
- [ ] Postman/Newman 冒烟 100% PASS
- [ ] k6 压测报告归档
- [ ] Prometheus 指标已接入
- [ ] ELK 日志通道打通
- [ ] OWASP 漏扫 0 高危
- [ ] 备份恢复演练通过
- [ ] 灰度计划已评审

---

## 4. 异常容错策略

### 4.1 故障场景矩阵
| 故障 | 应对策略 | 降级方案 |
|---|---|---|
| MySQL 短暂不可用 | HikariCP 重试 + Spring Retry（指数退避） | 切换读副本 |
| Redis 不可用 | Caffeine 本地缓存兜底 | 限流降级为单机版 |
| 导入大批量数据 | 异步队列 + 进度查询 | 用户拆批 |
| 导出超时 | SXSSF 流式 + 浏览器侧下载 | 分时段 |
| 分类删除冲突 | 业务级先迁移记录再删 | 提示用户 |
| Excel 公式注入 | 校验 cell.getCellType() == FORMULA → 直接拒 | 拒绝整文件 |
| JWT 密钥泄漏 | 紧急清空 auth_token 表 | 强制全用户重新登录 |
| 高并发写入 | 行级悲观锁 SELECT ... FOR UPDATE（仅余额敏感场景，本期不涉及） | 队列削峰 |
| OOM 风险 | SXSSF 流式 + 严格 `-Xmx` 上限 | 单文件 ≤ 5MB |
| CDN 异常 | 直接回源 | 静态资源改 Nginx 直出 |
| NTP 异常 | 服务端时间 + 客户端 token exp 容忍 60s | 拒绝请求 |

### 4.2 限流与隔离
- 登录：5 次/分钟/IP（防爆破）
- 普通接口：60 次/分钟/用户（防刷）
- 导出/导入：10 次/小时/用户（防资源滥用）
- 工具：Bucket4j（Redis 模式；本地降级 Caffeine）

### 4.3 降级开关
| 开关 | 默认 | 降级 |
|---|---|---|
| `feature.budget.warn` | ON | 关闭则超预算不预警 |
| `feature.audit.log` | ON | 关闭则不写 audit_log |
| `feature.excel.import` | ON | 关闭则返回 503 + 提示 |
| `feature.excel.export` | ON | 同上 |
| `rate.limit.enabled` | ON | 关闭则全放行（仅 dev） |

> 开关通过 `application-{profile}.yml` 配置

### 4.4 数据备份
- 每日 02:00 mysqldump → 本地 7 天轮转 + OSS 30 天归档
- 每周日全量备份 + binlog 增量
- 季度恢复演练（随机抽 1 天备份做恢复测试）

---

## 5. 安全设计

### 5.1 安全模型总览
| 维度 | 措施 | 实现层 |
|---|---|---|
| **传输安全** | 全站 HTTPS；HSTS | Nginx + Tomcat |
| **认证** | JWT HS256（7 天）；BCrypt cost=10 | JJWT + Spring Security |
| **授权** | URL 级 + 方法级 (`@PreAuthorize`) | Spring Security |
| **密码** | BCrypt；登录失败 5 次锁 30 分钟 | Service |
| **SQL 注入** | 全部 JPA 参数化；禁止 SQL 拼接 | 编码规范 + Spotless |
| **XSS** | 输出转义；CSP 头 | Nginx + 响应包装 |
| **CSRF** | Token 不放 Cookie；前后端分离天然免疫 | 架构 |
| **越权** | service 方法首行 `resource.userId == current.id` | 编码约束 |
| **审计** | 关键操作写 audit_log（180 天） | AOP + 定时清理 |
| **敏感字段** | password_hash `@JsonIgnore`；日志脱敏 | Jackson + Logback |
| **限流** | Bucket4j 令牌桶 | Filter |
| **依赖安全** | CI 跑 `dependency-check` (OWASP) | GitHub Actions |
| **密钥管理** | 密钥不进 Git；env 注入 | 部署规范 |
| **Header 安全** | X-Frame-Options / X-Content-Type-Options | Nginx |
| **输入验证** | Bean Validation + 自定义注解 | Validation |

### 5.2 JWT 详细策略
- 算法：HS256
- Claims：`uid` `role` `iat` `exp` `jti`
- 有效期：7 天
- 主动吊销：写 `auth_token.revoked=1`（改密 / 退出 / 管理员禁用）
- 续签：暂不实现（用户到期重新登录；二期可加 refresh）
- 密钥：32 字节随机；env 注入；定期轮换

### 5.3 密码策略
- 长度 8~32
- 必须含字母+数字
- BCrypt(cost=10)
- 不可与旧密码相同

### 5.4 数据保护
- 静态数据：MySQL 存储（无加密；MVP 阶段无敏感金融数据）
- 传输数据：全站 HTTPS
- 备份：mysqldump + OSS 加密

### 5.5 OWASP Top 10 自检
| 风险 | 措施 | 状态 |
|---|---|---|
| A01 访问控制失效 | 资源归属校验 | ✓ |
| A02 加密失效 | HTTPS + BCrypt | ✓ |
| A03 注入 | JPA 参数化 + 公式注入拦截 | ✓ |
| A04 不安全设计 | 架构评审 + 威胁建模 | ✓ |
| A05 安全配置错误 | profile 分离 + 默认拒绝 | ✓ |
| A06 易受攻击组件 | OWASP Dependency-Check | ✓ |
| A07 认证失效 | JWT + 锁 + 审计 | ✓ |
| A08 数据完整性 | 操作日志 + 软删除 | ✓ |
| A09 日志监控 | ELK + 告警 | ✓ |
| A10 SSRF | 本期无外部资源访问 | N/A |

---

## 6. 未来扩展预留设计

### 6.1 数据库层预留
- 所有表含 `created_at` / `updated_at`，支持软删除（`is_deleted` 模式已统一）
- 唯一键约束完整，避免后期数据清洗
- 枚举类型字段（`role` `status` `type` `pay_method`）保留扩展空间

### 6.2 业务层预留
| 预留点 | 触发条件 | 二期方向 |
|---|---|---|
| **多用户共享账本** | PRD 投票通过 | 增加 `book_member(book_id, user_id, role)` |
| **第三方登录** | 用户量 > 5K | Spring Authorization Server |
| **多币种** | 海外用户 | `currency` 字段已就位；补 `fx_rate` 服务 |
| **微信账单同步** | 用户呼声高 | 定时任务 + 微信商户 API |
| **票据 OCR** | 录入效率诉求 | 异步 AI 服务 + 反馈闭环 |
| **AI 自动分类** | 训练数据 1k+ | 异步 ML 服务 |
| **移动端 APP** | Web 流量 70% | Flutter / RN 复用 OpenAPI |
| **审计日志归档** | 单表 > 1M | ClickHouse / OSS 冷存储 |
| **灰度发布** | 团队 ≥ 5 人 | Spring Cloud LoadBalancer |
| **GraphQL** | 前端定制需求 | spring-graphql |
| **消息通知** | 用户要求 | notification 表 + WebSocket |
| **多端同步** | 设备 > 2 | 增量同步协议 (ETag / Cursor) |

### 6.3 架构演进路径
```
v1.0 单体 (本期)
   ↓ 用户量 > 10K
v2.0 模块化单体 (Spring Modulith)
   ↓ 用户量 > 100K
v3.0 微服务 (按业务域拆分)
   - 账号服务
   - 账本/记录服务
   - 统计服务
   - 导入导出服务
```

### 6.4 接口契约稳定性
- OpenAPI 3.0 文档为单一事实源
- 任何破坏性变更必须走 deprecation 流程：
  1. 新增 v2 接口
  2. v1 标 `deprecated: true` 保留 ≥ 6 个月
  3. 公告 → 灰度下线
- 字段新增走 optional；字段废弃保留 ≥ 1 个版本

---

## 7. 风险与缓解

| 风险 | 等级 | 缓解 |
|---|---|---|
| 客户需求频繁变更 | 中 | 严格变更控制；超出范围走商谈 |
| 性能不达标 | 中 | 关键 SQL 提前 Review；W6 提前压测 |
| 第三方依赖安全 | 中 | OWASP 漏扫常态化 |
| 人员流动 | 低 | 文档 + 代码 Review 制度 |
| 私有化部署环境差异 | 中 | Docker 化；标准化部署文档 |
| Excel 格式兼容性 | 高 | 多源样本库；导入预览 |
| 时区/日期处理 | 中 | 全站统一 UTC 存储 + ISO 8601 |

---

## 8. 上线验收标准（Definition of Done）

### 8.1 功能
- [ ] 33 个接口全部实现并通过冒烟
- [ ] 12 个原型页面 100% 可走通
- [ ] 28 条用户故事全部 Done Criteria 满足

### 8.2 质量
- [ ] 单元测试覆盖率 ≥ 70%
- [ ] 关键 service 覆盖率 ≥ 90%
- [ ] 无 P0/P1 缺陷遗留
- [ ] P2 缺陷 ≤ 5 个且有处理计划

### 8.3 性能
- [ ] P95 < 500ms
- [ ] 100 并发无 5xx
- [ ] 500 并发 P95 < 1s

### 8.4 安全
- [ ] OWASP Top 10 无高危
- [ ] 渗透测试通过
- [ ] JWT 密钥隔离

### 8.5 文档
- [ ] PRD / Scope / AC 三件套
- [ ] 技术方案 5 份
- [ ] OpenAPI 3.0 文档
- [ ] README + 部署文档
- [ ] Release Notes

### 8.6 部署
- [ ] docker-compose 一键启动
- [ ] 测试环境跑通
- [ ] 客户环境演练通过

---

## 9. 参考文档

| 文档 | 路径 |
|---|---|
| 产品需求 | [docs/prd/PRD.md](../docs/prd/PRD.md) |
| 范围界定 | [docs/prd/Scope.md](../docs/prd/Scope.md) |
| 验收标准 | [docs/prd/AC.md](../docs/prd/AC.md) |
| 架构与选型 | [01-architecture.md](./01-architecture.md) |
| 任务清单 | [02-tasks.md](./02-tasks.md) |
| 数据库脚本 | [03-db-schema.sql](./03-db-schema.sql) |
| OpenAPI 规范 | [04-api-spec.yaml](./04-api-spec.yaml) |
| 排期 | [05-schedule.md](./05-schedule.md) |

---

## 10. 签字

| 角色 | 姓名 | 签字 | 日期 |
|---|---|---|---|
| 项目经理 | 张三 | _ _ _ | 2026-06-15 |
| 架构师 | 李四 | _ _ _ | 2026-06-15 |
| 后端 TL | 王五 | _ _ _ | 2026-06-15 |
| 客户代表 | （待签） | _ _ _ | _ _ _ |
