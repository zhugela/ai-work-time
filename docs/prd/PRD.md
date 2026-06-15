# 个人记账后端 API - 产品需求文档（PRD）

| 文档版本 | v1.0 |
|---|---|
| 编制日期 | 2026-06-15 |
| 项目编号 | PJ-2026-06-15-JIZHANG |
| 项目类型 | Java SpringBoot 后端 API |
| 部署形态 | 私有化部署 |
| 目标用户 | 独立开发者/小型工作室（终端用户为个人记账人） |

---

## 1. 项目概述

### 1.1 项目背景
当前 C 端记账 APP 普遍存在「隐私收费、数据只入不出、广告侵扰、订阅涨价」四大痛点，市场对一款**数据自主可控、可二次开发、可私有部署**的记账底座存在明确需求。

### 1.2 项目目标
交付一套 Java SpringBoot 后端 API，提供账号体系、多账本、收支录入、分类、预算、统计、Excel 导入导出等完整能力，作为前端/小程序可调用的标准 REST 接口。

### 1.3 项目价值
- **对甲方（独立开发者）**：节省 2-3 个月自研成本，可直接商业化封装。
- **对终端记账人**：数据 100% 自主；按需部署；规避第三方隐私泄露。
- **对生态**：提供稳定 API 契约，便于 Web/iOS/Android/小程序多端扩展。

### 1.4 范围边界
- 本期仅交付**后端 API**，不包含前端 UI、不包含原生 APP。
- 仅支持单一终端用户注册使用，**不实现多用户共享账本/家庭协作**。

---

## 2. 角色定义

### 2.1 普通个人用户（USER）
- 唯一可在前台注册的角色
- 拥有全部记账功能权限
- 资源完全隔离（仅能访问自己的账本/记录/分类/预算）

### 2.2 系统管理员（ADMIN）
- 由运维/平台方预置，不开放注册
- 拥有用户启停、平台统计、审计日志查询、预置分类初始化能力
- **不可越权访问终端用户的业务数据**

---

## 3. 功能明细

### 3.1 账号域
| 编号 | 功能 | 接口 |
|---|---|---|
| F-A01 | 用户注册 | `POST /api/auth/register` |
| F-A02 | 用户登录 | `POST /api/auth/login` |
| F-A03 | 修改密码 | `PUT /api/auth/password` |
| F-A04 | 修改昵称 | `PUT /api/auth/profile` |
| F-A05 | 退出登录 | `POST /api/auth/logout` |
| F-A06 | 当前用户信息 | `GET /api/auth/me` |

### 3.2 账本域
| 编号 | 功能 | 接口 |
|---|---|---|
| F-B01 | 创建账本 | `POST /api/books` |
| F-B02 | 账本列表 | `GET /api/books` |
| F-B03 | 账本详情 | `GET /api/books/{id}` |
| F-B04 | 切换当前账本 | `POST /api/books/{id}/switch` |
| F-B05 | 归档/恢复账本 | `PUT /api/books/{id}/status` |

### 3.3 分类域
| 编号 | 功能 | 接口 |
|---|---|---|
| F-C01 | 分类列表 | `GET /api/categories?bookId=` |
| F-C02 | 新增分类 | `POST /api/categories` |
| F-C03 | 修改分类 | `PUT /api/categories/{id}` |
| F-C04 | 删除分类 | `DELETE /api/categories/{id}` |

### 3.4 收支录入域
| 编号 | 功能 | 接口 |
|---|---|---|
| F-T01 | 新增收支 | `POST /api/transactions` |
| F-T02 | 收支列表 | `GET /api/transactions?bookId=&from=&to=&categoryId=&page=&size=` |
| F-T03 | 收支详情 | `GET /api/transactions/{id}` |
| F-T04 | 修改收支 | `PUT /api/transactions/{id}` |
| F-T05 | 删除收支 | `DELETE /api/transactions/{id}` |

### 3.5 预算域
| 编号 | 功能 | 接口 |
|---|---|---|
| F-X01 | 设置/更新月度预算 | `PUT /api/budgets` |
| F-X02 | 查看预算执行进度 | `GET /api/budgets/progress?bookId=&yearMonth=` |

### 3.6 统计报表域
| 编号 | 功能 | 接口 |
|---|---|---|
| F-S01 | 月度收支汇总 | `GET /api/stats/monthly?bookId=&yearMonth=` |
| F-S02 | 分类支出占比 | `GET /api/stats/category-breakdown?bookId=&yearMonth=` |
| F-S03 | 收支趋势 | `GET /api/stats/trend?bookId=&rangeMonths=` |

### 3.7 数据导入导出域
| 编号 | 功能 | 接口 |
|---|---|---|
| F-E01 | 下载导入模板 | `GET /api/exports/template?bookId=` |
| F-E02 | Excel 批量导入 | `POST /api/imports/transactions` |
| F-E03 | 导出收支明细 | `GET /api/exports/transactions?bookId=&from=&to=` |
| F-E04 | 导出月度统计 | `GET /api/exports/monthly?bookId=&yearMonths=` |

### 3.8 管理员域
| 编号 | 功能 | 接口 |
|---|---|---|
| F-M01 | 平台规模统计 | `GET /api/admin/stats` |
| F-M02 | 启停用户 | `PUT /api/admin/users/{id}/enabled` |
| F-M03 | 审计日志查询 | `GET /api/admin/audit-logs` |

---

## 4. 业务规则

### 4.1 账号规则
- 用户名 4-20 位，唯一
- 密码 8-32 位，必须含字母+数字
- 密码 BCrypt 存储（cost=10）
- 登录失败 5 次锁定 30 分钟
- Token 默认 7 天有效；改密后所有 Token 失效

### 4.2 账本规则
- 单用户最多 50 个账本
- 至少保留 1 个活跃账本
- 归档账本只读，不可新增/修改/删除记录
- 默认币种 CNY

### 4.3 分类规则
- 系统预置 15 支出 + 8 收入
- 自定义分类：单账本最多 100 个
- 类型枚举：INCOME / EXPENSE / TRANSFER
- 录入时分类类型必须与收支类型一致

### 4.4 收支规则
- 金额 > 0 且 ≤ 9,999,999.99
- 日期不可超过今天 +7 天
- 支付方式枚举：CASH/ALIPAY/WECHAT/BANK_CARD/CREDIT_CARD/OTHER
- 删除采用软删除（is_deleted=1）

### 4.5 预算规则
- 一账本-分类-月唯一预算
- 金额 > 0；阈值默认 100%
- 预算仅对 EXPENSE 分类生效
- 录入后实时预警（软提示，不阻断）

### 4.6 统计规则
- 月份格式 YYYY-MM
- 统计范围仅含未软删记录
- 趋势按自然月聚合；空月份补 0
- 分类占比仅统计支出，返回 top10 + 其他

### 4.7 导入导出规则
- 文件 ≤ 5MB；单次 ≤ 5000 行
- 分类通过名称匹配（导入时）；不存在则报错并入失败列表
- 失败行不入库，返回 `{row, col, msg}` 列表
- Excel 公式注入需在导入时拦截（cell 首字符 `=` 视为非法）

---

## 5. 非功能性需求

### 5.1 性能
- 单接口 P95 响应时间 < 300ms
- 单账本 10 万条记录下，列表查询 < 500ms

### 5.2 安全
- 全站 HTTPS
- 密码 BCrypt 存储
- 接口需 Token；Token 签名校验
- 防 SQL 注入（参数化查询）
- Excel 导入防公式注入

### 5.3 可用性
- 服务可用性 ≥ 99.5%
- 接口幂等：登录、退出、Token 刷新

### 5.4 可维护性
- Java 17 + SpringBoot 3.x
- Maven 构建；统一异常处理；统一响应包装 `{code, msg, data}`
- 关键操作写 audit_log

### 5.5 数据
- 数据持久化到 MySQL 8.0（生产）；H2/SQLite（演示）
- 软删除字段统一为 `is_deleted`
- 时间字段统一 ISO 8601 字符串

### 5.6 部署
- Docker 镜像化
- 单体应用，无外部中间件强依赖（除数据库）
- 配置文件外置化

---

## 6. 接口通用约定

| 项 | 约定 |
|---|---|
| 协议 | HTTPS + REST |
| 鉴权 | `Authorization: Bearer <token>` |
| 时间格式 | ISO 8601（`2026-06-15T10:00:00+08:00`） |
| 金额 | 字符串返回（避免 JS 精度丢失） |
| 分页 | `page` 从 1 起；`size` 默认 20，最大 100 |
| 响应包装 | `{"code":0, "msg":"ok", "data":{...}}` |
| 业务错误码 | 非 0 即业务异常，详见各接口注释 |

---

## 7. 术语表

| 术语 | 释义 |
|---|---|
| 账本（Book） | 用户自定义的一组收支记录的容器 |
| 分类（Category） | 收支的语义分组（餐饮/工资等） |
| 收支（Transaction） | 单笔进账或出账记录 |
| 预算（Budget） | 某分类在某月的金额上限 |
| 软删除 | 通过 `is_deleted=1` 标记，不物理删除 |