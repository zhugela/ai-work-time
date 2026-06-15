# 个人记账后端管理系统 · 原型交付包

> 项目编号 **PJ-2026-06-15-JIZHANG** · 版本 v1.0 · 交付日期 2026-06-15

---

## 1. 交付清单

| 类别 | 路径 | 说明 |
|---|---|---|
| **设计规范** | [`design/`](design/) | 设计系统 + 预览页 + 示例 + 缺口文档 |
| ↳ | [DESIGN.md](design/DESIGN.md) | 完整设计系统（颜色/字体/间距/圆角/阴影/组件/可访问性） |
| ↳ | [preview.html](design/preview.html) | 明色主题预览（颜色色板/字体/组件展示） |
| ↳ | [preview-dark.html](design/preview-dark.html) | 暗色主题预览 |
| ↳ | [example.html](design/example.html) | 登录页完整实现示例 |
| ↳ | [DESIGN_GAPS.md](design/DESIGN_GAPS.md) | 15 项设计缺口与待评审项 |
| **HTML 原型** | [`prototype/`](prototype/) | 12 个核心页面 + 全局资源 + 截图 + 评审 PPT |
| ↳ | `assets/app.css` | 全局设计系统（仅一份 token + 组件样式） |
| ↳ | `assets/app.js` | 通用交互：主题切换 / Toast / 弹窗 / 表单校验 / 路由 |
| ↳ | `login.html` | 01 登录页 |
| ↳ | `register.html` | 02 注册页 |
| ↳ | `settings.html` | 03 个人设置（含 4 个 tab：资料/改密/偏好/危险区） |
| ↳ | `books.html` | 04 账本列表（活跃+归档，新建账本弹窗） |
| ↳ | `transaction-new.html` | 05 收支录入（类型切换+分类网格+超预算预警） |
| ↳ | `categories.html` | 06 收支分类管理（支出/收入 tab，预置+自定义） |
| ↳ | `budgets.html` | 07 月度预算配置（3 张概览+5 分类进度条） |
| ↳ | `transactions.html` | 08 收支明细列表（多维筛选+分页） |
| ↳ | `stats-monthly.html` | 09 月度统计（3 大数字+结构+近 3 月对比） |
| ↳ | `stats-breakdown.html` | 10 分类占比（纯 SVG 环形+横向条形） |
| ↳ | `stats-trend.html` | 11 收支趋势（纯 SVG 三折线+月度表） |
| ↳ | `import-export.html` | 12 导入导出（拖拽上传+错误行预览+历史） |
| ↳ | [`review-deck.html`](prototype/review-deck.html) | **24 张评审演示 PPT**（键盘/触屏/wheel 全支持） |
| ↳ | `screenshots/` | 15 张自动化截图（12 主页 + 预算超支 + 新建弹窗 + 导入预览） |
| **数据模型** | sqlite-mcp 已落地 8 张表 | sys_user / book / category / tx_record / budget / auth_token / audit_log / sqlite_sequence |
| **需求文档** | [`docs/prd/`](docs/prd/) | PRD.md + Scope.md + AC.md 三件套 |

---

## 2. 快速预览

### 2.1 启动本地服务（推荐）
```bash
cd prototype
python -m http.server 8123 --bind 127.0.0.1
# 浏览器打开
# http://127.0.0.1:8123/login.html
```

### 2.2 直接打开（无服务）
> Windows 双击 `prototype/login.html` 即可在浏览器中查看，链接跳转走相对路径，但需要保留 `assets/` 子目录。

### 2.3 评审 PPT
```bash
# 启动服务后访问
http://127.0.0.1:8123/review-deck.html
# 操作：←/→ 翻页 · Space 下一张 · Home/End 首末 · F11 全屏
```

---

## 3. 设计系统快速参考

| 维度 | 主值 |
|---|---|
| 主色 | `#3B6EE8`（brand-500） |
| 收入色 | `#1FB85B` |
| 支出色 | `#E5484D` |
| 预警色 | `#F5A524` |
| 圆角 | 4 / 6 / 8 / 12 / 16 / full |
| 阴影 | 5 阶（xs/sm/md/lg/xl） |
| 间距 | 4 倍数（4/8/12/16/20/24/32/40/48/64） |
| 字号 | display 32 / h1 24 / h2 20 / h3 18 / body 14 / caption 12 |
| 主题 | 浅色 / 暗色 / 跟随系统（localStorage 持久化） |

> 详见 [DESIGN.md](design/DESIGN.md)

---

## 4. 原型 ↔ PRD ↔ AC 对照

| 页面 | 文件 | 对应 US | AC 关键点 |
|---|---|---|---|
| 登录 | `login.html` | US-01, 02 | 必填校验 / 失败 5 次锁定 |
| 注册 | `register.html` | US-01 | 密码 8-32 + 字母数字 |
| 个人设置 | `settings.html` | US-03, 05 | 改密 Token 失效 / 退出幂等 |
| 账本列表 | `books.html` | US-06, 07, 08 | 50 上限 / 最后活跃保护 |
| 收支录入 | `transaction-new.html` | US-09, 10, 11, 17 | 类型一致 / 超预算预警 |
| 分类管理 | `categories.html` | US-13, 14 | 预置只读 / 自定义 100 |
| 月度预算 | `budgets.html` | US-15, 16, 17 | 三色进度 / 阈值配置 |
| 收支明细 | `transactions.html` | US-12 | 软删除过滤 / 分页 |
| 月度统计 | `stats-monthly.html` | US-18, 19 | 占比合计 100% |
| 分类占比 | `stats-breakdown.html` | US-19 | top10 + 其他 |
| 收支趋势 | `stats-trend.html` | US-20 | 6/12 月切换 |
| 导入导出 | `import-export.html` | US-21~24 | 5MB/5000/公式拦截 |

---

## 5. 自动化校验记录

通过 playwright-mcp 已完成：

- ✅ 12 个主页面均渲染成功（无 JS 错误，仅 favicon 404 可忽略）
- ✅ 全局布局：侧边栏 + 顶部栏 + 内容区 在 1440×900 视口下完整呈现
- ✅ 关键交互：录入页分类网格、预算进度条三色、占比 SVG 环形、趋势三折线、导入拖拽
- ✅ 弹窗显隐：新建账本 / 分类编辑 / 导入预览 / 导出成功 全部正常
- ✅ 截图留存：`screenshots/01-login.png` ~ `15-import-preview.png`

---

## 6. 评审待确认（15 项）

详见 [DESIGN_GAPS.md](design/DESIGN_GAPS.md) 与评审 PPT 第 21-22 页。

> **关键 3 项**：
> - G-01 顶部账本切换器位置（侧边栏顶部 vs 顶部栏左侧）
> - G-04 收支色彩编码（收入=绿、支出=红、转账=蓝；本期不做转账）
> - G-07 趋势图图表库（当前纯 SVG；备选 ECharts / Chart.js）

---

## 7. 目录树

```
D:\code\account-book-project
├─ docs/
│  ├─ research/                     # 立项评估
│  └─ prd/                          # PRD / Scope / AC
├─ design/                          # 设计规范包
│  ├─ DESIGN.md
│  ├─ preview.html
│  ├─ preview-dark.html
│  ├─ example.html
│  └─ DESIGN_GAPS.md
├─ prototype/                       # HTML 原型
│  ├─ assets/
│  │  ├─ app.css                    # 设计系统 + 通用组件
│  │  └─ app.js                     # 主题/弹窗/Toast/校验
│  ├─ login.html                    # 01
│  ├─ register.html                 # 02
│  ├─ settings.html                 # 03
│  ├─ books.html                    # 04
│  ├─ transaction-new.html          # 05
│  ├─ categories.html               # 06
│  ├─ budgets.html                  # 07
│  ├─ transactions.html             # 08
│  ├─ stats-monthly.html            # 09
│  ├─ stats-breakdown.html          # 10
│  ├─ stats-trend.html              # 11
│  ├─ import-export.html            # 12
│  ├─ review-deck.html              # 评审 PPT (24 张)
│  └─ screenshots/                  # 自动化截图 15 张
└─ README.md                        # 本文件
```

---

## 8. 验收清单（Release Gates for Prototype）

- [x] 设计系统（DESIGN.md）覆盖 8 大维度
- [x] 设计规范包 5 份文件齐全
- [x] 12 个核心页面 100% 落地
- [x] 全部页面遵循同一份 `assets/app.css` token
- [x] 表单校验、弹窗、Toast 通用交互封装
- [x] 12 张主截图 + 3 张交互截图
- [x] 评审 PPT 24 张幻灯片
- [x] 数据表 8 张已通过 sqlite-mcp 自洽性校验
- [x] 文档三件套（PRD/Scope/AC）齐备

---

## 9. 联系与变更

- 文档变更需走 PRD 评审并标注版本号
- 任何 In-Scope 新增项需评估影响范围
- 设计 token 调整需同步更新 `assets/app.css` 与 `DESIGN.md`

**编制**：Claude（MiniMax-M3）· 2026-06-15
