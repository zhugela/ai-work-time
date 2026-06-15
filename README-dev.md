# 个人记账后端 · 开发与启动说明

> 配套：[tech-spec.md](../tech-spec.md) · [OpenAPI 规范](../04-api-spec.yaml) · [数据库脚本](../03-db-schema.sql)

## 一、环境要求

| 组件 | 版本 | 备注 |
|---|---|---|
| JDK | 17 LTS | 必需 |
| Maven | 3.9+ | 已包含 `mvnw` 可选 |
| MySQL | 8.0+ | 生产环境；测试用 H2 内存库 |
| Redis | 7.x | 可选（Token 黑名单 + 限流） |
| RabbitMQ | 3.12+ | 可选（异步任务） |

> 本地无需安装 MySQL/Redis 即可启动 — SpringBoot profile `dev` 自动切换 H2 内存库，Redis/RabbitMQ 缺失时自动降级。

## 二、快速启动（dev 模式）

```bash
# 1. 进入后端目录
cd backend

# 2. 编译 + 运行测试
mvn -B clean test

# 3. 启动应用（dev 模式默认 H2 + 内存）
mvn spring-boot:run

# 4. 访问服务
open http://localhost:8080/swagger-ui.html
```

启动成功后控制台会看到：
```
Started JzApplication in 3.456 seconds
Tomcat started on port 8080
```

## 三、prod 模式（MySQL）

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE personal_jz DEFAULT CHARACTER SET utf8mb4;"

# 2. 设置环境变量（或修改 application-prod.yml）
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=personal_jz
export DB_USER=root
export DB_PASSWORD=yourpass
export JJWT_SECRET="$(openssl rand -base64 48)"  # 至少 32 字节
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export SPRING_PROFILES_ACTIVE=prod

# 3. 启动
mvn -B clean package -DskipTests
java -jar target/jz-backend.jar
```

Flyway 会自动执行 `V1__init.sql` + `V2__seed_categories.sql`。

## 四、测试访问地址

| 资源 | URL |
|---|---|
| 应用入口 | http://localhost:8080/api/... |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Knife4j 增强 | http://localhost:8080/doc.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| H2 控制台（仅 dev） | http://localhost:8080/h2-console |
| 健康检查 | http://localhost:8080/actuator/health |

## 五、核心 API 路径速查

| 模块 | 路径前缀 |
|---|---|
| 认证 | `/api/auth/{register,login,logout,me,password,profile}` |
| 账本 | `/api/books` |
| 分类 | `/api/categories` |
| 收支 | `/api/transactions` |
| 预算 | `/api/budgets`, `/api/budgets/progress` |
| 统计 | `/api/stats/{monthly,category-breakdown,trend}` |
| 导入导出 | `/api/exports/*`, `/api/imports/*` |
| 管理员 | `/api/admin/{stats,users,audit-logs}` |

详细契约见 `tech-design/04-api-spec.yaml`。

## 六、测试

```bash
# 全量测试
mvn test

# 单模块
mvn test -Dtest=AuthServiceTest

# 覆盖率（需 jacoco 插件）
mvn test jacoco:report
# 报告路径：target/site/jacoco/index.html
```

## 七、数据库迁移

| 文件 | 用途 |
|---|---|
| `src/main/resources/db/migration/V1__init.sql` | 8 张业务表 + 索引 + 约束 |
| `src/main/resources/db/migration/V2__seed_categories.sql` | 23 条系统预置分类 |

新增迁移文件命名格式：`V{版本}__{描述}.sql`，Flyway 自动按版本号顺序执行。

## 八、目录结构

```
backend/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/personal/jz/
│   │   │   ├── JzApplication.java          # 启动类
│   │   │   ├── common/                      # 公共层
│   │   │   │   ├── api/                    # 响应包装
│   │   │   │   ├── audit/                  # 审计 AOP
│   │   │   │   ├── exception/              # 异常体系
│   │   │   │   ├── security/               # @CurrentUser
│   │   │   │   └── validation/             # 自定义校验
│   │   │   ├── config/                     # 配置（安全/缓存/OpenAPI）
│   │   │   ├── security/                   # JWT 工具 + 过滤器
│   │   │   ├── entity/                     # 8 张表 MyBatis-Plus 实体
│   │   │   ├── repository/                 # MyBatis-Plus BaseMapper
│   │   │   ├── module/                     # 8 业务模块
│   │   │   │   ├── auth/                   # 注册/登录/改密
│   │   │   │   ├── book/                   # 账本
│   │   │   │   ├── category/               # 分类
│   │   │   │   ├── tx/                     # 收支
│   │   │   │   ├── budget/                 # 预算
│   │   │   │   ├── stats/                  # 统计
│   │   │   │   ├── imexport/               # 导入导出
│   │   │   │   └── admin/                  # 管理员
│   │   │   └── util/                       # 工具类
│   │   └── resources/
│   │       ├── application*.yml            # 多环境配置
│   │       ├── logback-spring.xml
│   │       └── db/migration/               # Flyway 脚本
│   └── test/java/com/personal/jz/          # 单元/集成测试
```

## 九、常见问题

| 问题 | 解决 |
|---|---|
| 启动报 `JJWT secret too short` | 至少 32 字节；通过 `JJWT_SECRET` 环境变量覆盖 |
| 启动报 `Redis connection refused` | dev 模式已 `optional=true`，降级到 Caffeine；不影响功能 |
| 测试报 `Bean creation failed` | 检查 `@SpringBootTest` 是否引用了完整应用上下文 |
| 端口 8080 被占用 | `server.port=8081 -D` 参数覆盖 |
| Flyway 失败 | 确认 `V{版本}__xxx.sql` 命名格式；不可修改已执行的脚本 |

## 十、提交规范

采用 Conventional Commits：

```bash
git commit -m "feat(auth): implement register with auto default book"
git commit -m "fix(budget): clamp warn threshold to 0-200 range"
git commit -m "docs: update README-dev startup steps"
```

Type 枚举：`feat` / `fix` / `refactor` / `perf` / `test` / `docs` / `chore` / `ci` / `build`

Scope 枚举：见各模块包名（auth / book / category / tx / budget / stats / imexport / admin / security 等）
