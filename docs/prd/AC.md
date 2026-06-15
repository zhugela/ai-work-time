# 个人记账后端 API - 验收标准文档（AC）

| 文档版本 | v1.0 |
|---|---|
| 编制日期 | 2026-06-15 |
| 配套 PRD | PRD.md v1.0 |
| 配套范围 | Scope.md v1.0 |

---

## 通用验收约定

每条用户故事的 Done Criteria 包含三类检查点：
- **功能正确性（Functional）**：核心业务结果是否符合预期
- **校验与异常（Validation）**：非法入参是否被正确拦截
- **非功能性（NFR）**：性能/安全/可维护性是否达标

仅当三类检查点全部 PASS，方可标记该条用户故事 Done。

---

## A. 普通个人用户

### US-01 用户注册
- [F] 注册成功后 `sys_user` 新增 1 条记录；密码以 BCrypt 散列存储；自动创建「日常账本」
- [F] 注册成功后 `auth_token` 立即下发 1 个有效 Token
- [V] 用户名 < 4 或 > 20 位 → 400 `INVALID_USERNAME`
- [V] 密码 < 8 位或仅含字母/仅含数字 → 400 `WEAK_PASSWORD`
- [V] 手机号/邮箱格式错 → 400 `INVALID_CONTACT`
- [V] 两次密码不一致 → 400 `PASSWORD_MISMATCH`
- [V] 用户名已存在 → 409 `USERNAME_TAKEN`
- [N] 接口 P95 < 500ms

### US-02 用户登录
- [F] 账号密码正确 → 200 + Token + 用户基本信息
- [F] `sys_user.last_login_at` 自动更新
- [V] 账号不存在 / 密码错 → 统一 401 `INVALID_CREDENTIALS`（不区分）
- [V] 账号被禁用 → 403 `ACCOUNT_DISABLED`
- [V] 连续失败 5 次 → 423 `ACCOUNT_LOCKED`，30 分钟内拒绝登录
- [N] 错误响应时间 < 200ms

### US-03 修改密码 / 昵称
- [F] 改密成功 → 所有 Token 标记 revoked=1
- [F] 改昵称成功 → `GET /api/auth/me` 立即返回新昵称
- [V] 原密码错 → 401 `WRONG_OLD_PASSWORD`
- [V] 新密码强度不足 → 400 `WEAK_PASSWORD`
- [V] 新旧密码相同 → 400 `SAME_PASSWORD`

### US-04 Token 维持登录
- [F] 携带有效 Token → 业务接口 200
- [F] Token 即将过期前可刷新续期
- [V] Token 缺失/格式错 → 401 `TOKEN_MISSING/INVALID`
- [V] Token 过期 → 401 `TOKEN_EXPIRED`
- [V] Token 已被撤销（改密/退出/管理员禁用）→ 401 `TOKEN_REVOKED`

### US-05 退出登录
- [F] 退出后该 Token 立即无法访问受保护接口
- [F] 重复退出幂等返回 200
- [N] `audit_log` 写入 `LOGOUT` 动作

### US-06 创建账本
- [F] 账本创建成功，默认币种 CNY，状态 ACTIVE
- [F] 自动复制系统预置分类到该账本
- [V] 账本名为空或 > 20 字 → 400
- [V] 同用户下重名 → 409 `BOOK_NAME_TAKEN`
- [V] 币种非枚举值 → 400 `UNSUPPORTED_CURRENCY`
- [V] 单用户账本数 ≥ 50 → 400 `BOOK_LIMIT_REACHED`

### US-07 编辑/归档账本
- [F] 归档账本后 `status=ARCHIVED`，列表可查但 `is_active=false`
- [F] 归档账本内**不可新增/修改/删除记录**
- [F] 恢复账本 → 状态恢复 ACTIVE
- [V] 归档最后一个活跃账本 → 400 `LAST_ACTIVE_BOOK`
- [V] 操作他人账本 → 404

### US-08 切换当前账本
- [F] 切换成功 → `sys_user.current_book_id` 更新
- [V] 目标账本非本人/已归档 → 400/404

### US-09 录入支出
- [F] 支出分类、金额、日期必填项正确入库
- [F] 默认 `pay_method=CASH`；时间未填=当前时刻
- [V] 金额 ≤ 0 或 > 9,999,999.99 → 400 `INVALID_AMOUNT`
- [V] 分类 ID 不存在或不属于本账本 → 400 `CATEGORY_NOT_FOUND`
- [V] 分类类型=INCOME → 400 `CATEGORY_TYPE_MISMATCH`
- [V] 日期 > 今天+7 天 → 400 `DATE_OUT_OF_RANGE`
- [V] 账本已归档 → 400 `BOOK_ARCHIVED`
- [N] 单次录入 P95 < 200ms

### US-10 录入收入
- [F] 收入分类匹配 `type=INCOME`，记录正常入库
- [V] 分类类型=EXPENSE → 400 `CATEGORY_TYPE_MISMATCH`
- [V] 其余校验同 US-09

### US-11 修改/删除收支
- [F] 修改成功后 `updated_at` 自动更新
- [F] 删除采用软删除（`is_deleted=1`），不在列表中返回
- [V] 操作他人记录 → 404
- [V] 归档账本内记录 → 400 `BOOK_ARCHIVED`

### US-12 收支明细列表
- [F] 默认按 `occurred_at DESC` 排序
- [F] 支持 `bookId/from/to/categoryId/page/size` 多维筛选
- [V] `size < 1` 或 `> 100` → 400
- [V] `from > to` → 400
- [N] 10 万条数据下查询 < 500ms

### US-13 使用系统预置分类
- [F] 新用户首次登录即可在分类列表看到所有预置项
- [V] 预置分类不可删除 → 接口返回 400 `SYSTEM_CATEGORY_READONLY`

### US-14 自定义分类
- [F] 用户级分类可增/改/删
- [V] 同账本下重名 → 409 `CATEGORY_NAME_TAKEN`
- [V] 颜色非 hex → 400 `INVALID_COLOR`
- [V] 单账本自定义数 ≥ 100 → 400 `CATEGORY_LIMIT_REACHED`

### US-15 设置月度预算
- [F] 同账本-分类-月份唯一预算记录
- [F] 同月份再次设置 → 覆盖更新
- [V] 预算金额 ≤ 0 → 400 `INVALID_AMOUNT`
- [V] 月份格式非 YYYY-MM → 400 `INVALID_MONTH`
- [V] 分类类型=INCOME → 400 `BUDGET_ON_INCOME`

### US-16 查看预算进度
- [F] 返回 `{categoryId, categoryName, budget, used, progress, status}`
- [F] 未设置预算的分类 → `progress=null`
- [V] 月份格式错 → 400

### US-17 超预算预警
- [F] 录入返回体含 `overBudgetWarning: true/false`
- [F] 阈值可通过 `warn_threshold` 配置（0~200%）
- [N] 预警仅响应字段提示，不抛异常

### US-18 月度收支汇总
- [F] 返回 `income / expense / balance`
- [F] 软删除记录不计入
- [V] 月份格式错 → 400

### US-19 分类占比
- [F] 返回 `[{categoryId, name, amount, percentage}], top10 + other`
- [F] 占比合计 = 100%（保留 2 位小数）
- [V] 月份格式错 → 400

### US-20 收支趋势
- [F] 支持 `rangeMonths = 6 / 12`
- [F] 空月份补 0
- [V] 范围非法 → 默认 6

### US-21 下载导入模板
- [F] 返回 `.xlsx` 文件，包含表头 + 1 行示例
- [F] 表头固定：`日期 / 金额 / 类型 / 分类名 / 支付方式 / 备注`
- [V] `bookId` 缺失 → 400

### US-22 Excel 批量导入
- [F] 返回 `{success: N, failed: [{row, col, msg}]}`
- [F] 文件 > 5MB → 400 `FILE_TOO_LARGE`
- [F] 行数 > 5000 → 400 `ROW_LIMIT`
- [V] 非 xlsx 文件 → 400 `UNSUPPORTED_FILE_TYPE`
- [V] 表头错 → 400 `INVALID_HEADER`
- [V] 分类名在本账本不存在 → 行级失败
- [V] 金额非法 → 行级失败
- [V] 日期格式错 → 行级失败
- [V] cell 首字符为 `=` → 400 `FORMULA_INJECTION`
- [N] 失败行不入库；成功行原子写入

### US-23 导出收支明细
- [F] 返回 `.xlsx`，列：`日期/类型/分类/金额/支付方式/备注`
- [F] 0 条记录 → 仅表头
- [V] 跨度 > 1 年 → 400 `RANGE_TOO_LONG`

### US-24 导出月度统计
- [F] 多月份合并为多 Sheet，每 Sheet 一月
- [V] 月份范围 > 12 → 400

---

## B. 系统管理员

### US-25 预置分类初始化
- [F] 启动时幂等写入 15 支出 + 8 收入
- [F] `is_system=1`
- [V] 重复启动不抛异常，仅 skip

### US-26 平台规模统计
- [F] 返回 `{userCount, bookCount, txCount, activeUserCount}`
- [N] 单次聚合 < 1s

### US-27 启停账号
- [F] 禁用用户后该用户所有 Token 失效
- [F] 启用后用户可重新登录
- [V] 停用自己 → 400 `CANNOT_DISABLE_SELF`
- [V] 目标用户不存在 → 404

### US-28 审计日志
- [F] 关键操作（登录/退出/录入/导出/管理动作）均写日志
- [F] 保留 180 天
- [F] 支持按用户/操作类型/时间筛选
- [N] 10 万条数据查询 < 1s

---

## 全局验收（Release Gates）

| 项 | 标准 |
|---|---|
| 单元测试覆盖率 | service 层 ≥ 70% |
| 接口冒烟 | 全部 Must have 接口 100% 通过 Postman / Newman 自动化 |
| 安全扫描 | OWASP Top 10 全部无高危漏洞 |
| 性能压测 | 100 并发下 P95 < 500ms；500 并发下无 5xx |
| 文档完备 | PRD / Scope / AC / OpenAPI YAML 四件套齐备 |
| Docker 镜像 | `docker-compose up` 一键拉起 MySQL + 应用 |
| 数据迁移 | 提供空库初始化 SQL + 示例数据 SQL |