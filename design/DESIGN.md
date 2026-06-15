# 个人记账后端管理系统 · 设计系统规范（DESIGN.md）

| 文档版本 | v1.0 |
|---|---|
| 编制日期 | 2026-06-15 |
| 配套 PRD | docs/prd/PRD.md |
| 平台 | Web（PC 优先，桌面宽 1280~1440） |
| 主题 | 明色 / 暗色 双主题 |

---

## 0. 设计理念

> **专业、克制、清晰、低干扰**。
> 个人记账是高频短时长操作，UI 必须"安静"——大量留白、单一信息焦点、避免装饰。
> 颜色用作**信号**而非装饰：红=支出、绿=收入、橙=预警、蓝=链接/操作。

---

## 1. 颜色系统

### 1.1 品牌主色（Brand）
| Token | 浅色 | 暗色 | 用途 |
|---|---|---|---|
| `--brand-50` | `#EEF4FF` | `#1A2438` | 链接 hover 背景 |
| `--brand-100` | `#D9E6FF` | `#1F2C44` | 选中态背景 |
| `--brand-300` | `#7CA6F5` | `#5C84D9` | 边框强调 |
| `--brand-500` | `#3B6EE8` | `#4A7BEE` | **主色 / Primary** |
| `--brand-600` | `#2E5BC6` | `#3D6CD9` | hover |
| `--brand-700` | `#1F479F` | `#2A5AC0` | active |

### 1.2 业务语义色（Semantic）
| Token | 浅色 | 暗色 | 用途 |
|---|---|---|---|
| `--income-500` | `#1FB85B` | `#2BD372` | 收入、收入数字、正向结余 |
| `--income-100` | `#E6F8EE` | `#143A26` | 收入背景 |
| `--expense-500` | `#E5484D` | `#F26E73` | 支出、支出数字、超预算 |
| `--expense-100` | `#FCEDED` | `#3F1F22` | 支出背景 |
| `--warn-500` | `#F5A524` | `#F7B94D` | 预算预警 |
| `--warn-100` | `#FEF6E6` | `#3D2F12` | 预警背景 |
| `--info-500` | `#0EA5E9` | `#38BDF8` | 信息提示 |
| `--success-500` | `#1FB85B` | `#2BD372` | 成功（同 income） |
| `--danger-500` | `#E5484D` | `#F26E73` | 错误（同 expense） |

### 1.3 中性色阶（Neutral / Gray）
> 共 12 阶，从 0 到 1000

| Token | 浅色 | 暗色 |
|---|---|---|
| `--n-0` | `#FFFFFF` | `#0B0F1A` |
| `--n-25` | `#FAFBFC` | `#0F1421` |
| `--n-50` | `#F5F7FA` | `#141A2A` |
| `--n-100` | `#EEF1F6` | `#1A2236` |
| `--n-200` | `#E1E6EF` | `#222B40` |
| `--n-300` | `#C9D1DE` | `#2C3650` |
| `--n-400` | `#9AA4B5` | `#3D4863` |
| `--n-500` | `#6B7585` | `#5A6480` |
| `--n-600` | `#4B5563` | `#7E8AA0` |
| `--n-700` | `#374151` | `#A5AEBD` |
| `--n-800` | `#1F2937` | `#D5DAE3` |
| `--n-900` | `#111827` | `#EDF0F5` |
| `--n-1000` | `#000000` | `#FFFFFF` |

### 1.4 文字色阶
| Token | 浅色 | 暗色 | 用途 |
|---|---|---|---|
| `--text-primary` | `--n-900` | `--n-900(D)` | 标题、正文 |
| `--text-secondary` | `--n-700` | `--n-800(D)` | 副文、说明 |
| `--text-tertiary` | `--n-500` | `--n-600(D)` | 占位、辅助 |
| `--text-disabled` | `--n-400` | `--n-500(D)` | 禁用 |
| `--text-on-brand` | `#FFFFFF` | `#FFFFFF` | 主色按钮文字 |
| `--text-link` | `--brand-500` | `--brand-500(D)` | 链接 |

### 1.5 边框 & 分割
| Token | 浅色 | 暗色 |
|---|---|---|
| `--border-default` | `--n-200` | `--n-300(D)` |
| `--border-strong` | `--n-300` | `--n-400(D)` |
| `--divider` | `--n-100` | `--n-200(D)` |

### 1.6 表面（Surface）
| Token | 浅色 | 暗色 |
|---|---|---|
| `--bg-page` | `--n-50` | `--n-0(D)` |
| `--bg-elevated` | `#FFFFFF` | `--n-25(D)` |
| `--bg-overlay` | `rgba(17,24,39,0.5)` | `rgba(0,0,0,0.7)` |

---

## 2. 字体系统

### 2.1 字体族
- 主字体：`"Inter", "PingFang SC", "Microsoft YaHei", system-ui, sans-serif`
- 数字字体：`"Inter", "DIN Alternate", "JetBrains Mono", monospace`（金额、时间、等宽数字）

### 2.2 字号层级
| Token | size / line-height | 用途 |
|---|---|---|
| `--fs-display` | 32 / 40 | 报表大数字、登录页 LOGO |
| `--fs-h1` | 24 / 32 | 页面主标题 |
| `--fs-h2` | 20 / 28 | 区块标题 |
| `--fs-h3` | 18 / 26 | 卡片标题 |
| `--fs-body-lg` | 16 / 24 | 大号正文 |
| `--fs-body` | 14 / 22 | **默认正文** |
| `--fs-body-sm` | 13 / 20 | 辅助说明 |
| `--fs-caption` | 12 / 18 | 标签、注释 |
| `--fs-mono-lg` | 28 / 36 | 大金额数字 |
| `--fs-mono` | 16 / 24 | 表格金额 |

### 2.3 字重
- `400` Regular：正文
- `500` Medium：表格表头、按钮
- `600` Semibold：标题
- `700` Bold：报表大数字

---

## 3. 间距系统（Spacing）

> 4 倍数制（4px 基准）

| Token | 值 | 典型用途 |
|---|---|---|
| `--space-1` | 4 | 图标内边距、紧凑间距 |
| `--space-2` | 8 | 表单内边距 |
| `--space-3` | 12 | 按钮内边距、卡片内 padding |
| `--space-4` | 16 | **标准内边距 / 段落间距** |
| `--space-5` | 20 | 卡片之间 |
| `--space-6` | 24 | 区块之间 |
| `--space-8` | 32 | 大区块 |
| `--space-10` | 40 | 页面边距 |
| `--space-12` | 48 | 顶部留白 |
| `--space-16` | 64 | 大留白 |

---

## 4. 圆角（Radius）

| Token | 值 | 用途 |
|---|---|---|
| `--radius-xs` | 4 | Tag、徽标 |
| `--radius-sm` | 6 | 输入框、小按钮 |
| `--radius-md` | 8 | 按钮、卡片 |
| `--radius-lg` | 12 | 弹窗、模态 |
| `--radius-xl` | 16 | 大卡片 |
| `--radius-full` | 9999 | 头像、胶囊按钮 |

---

## 5. 阴影（Elevation）

| Token | 值 | 用途 |
|---|---|---|
| `--shadow-xs` | `0 1px 2px rgba(15,23,42,0.05)` | 微弱浮起 |
| `--shadow-sm` | `0 1px 3px rgba(15,23,42,0.08), 0 1px 2px rgba(15,23,42,0.04)` | 卡片 |
| `--shadow-md` | `0 4px 12px rgba(15,23,42,0.08), 0 2px 4px rgba(15,23,42,0.04)` | 悬浮 |
| `--shadow-lg` | `0 12px 32px rgba(15,23,42,0.12), 0 4px 8px rgba(15,23,42,0.04)` | 弹窗 |
| `--shadow-xl` | `0 24px 48px rgba(15,23,42,0.18)` | 重要弹层 |

---

## 6. 布局系统

### 6.1 栅格
- 12 栏；`gap: 24px`
- 内容最大宽度 `1200px`；左右页边距 `40px`
- 弹窗最大宽度 `560px`（表单）、`800px`（确认）、`960px`（含图表）

### 6.2 全局框架
- 顶部栏：高度 `64px`，固定吸顶
- 侧边栏：宽 `240px`，可折叠至 `64px`
- 内容区：`min-height: calc(100vh - 64px)`
- 页脚：可选，`48px`

---

## 7. 通用组件规范

### 7.1 按钮（Button）
| 状态 | 主色 | 次色 | 危险 | 幽灵 | 文字 |
|---|---|---|---|---|---|
| 默认 | bg=brand-500, text=白色 | bg=neutral-100, text=primary | bg=expense-500, text=白色 | bg=透明, border=neutral-300, text=primary | bg=透明, text=brand-500 |
| hover | bg=brand-600 | bg=neutral-200 | bg=#c93b40 | bg=neutral-50 | text=brand-600 |
| active | bg=brand-700 | bg=neutral-300 | bg=#a32f33 | bg=neutral-100 | text=brand-700 |
| disabled | bg=neutral-200, text=neutral-400 | — | — | — | text=neutral-400 |

- 尺寸：`sm 32px / md 40px / lg 48px`
- 圆角：`--radius-md`
- 内边距：`x=16, y=0`（sm x=12）
- 字体：`--fs-body / 500`
- 焦点环：`outline: 2px solid --brand-300; outline-offset: 2px;`
- 加载态：内置 spinner，禁用点击

### 7.2 输入框（Input）
- 高度：`md 40px / lg 48px`
- 内边距：`x=12, y=0`
- 圆角：`--radius-sm`
- 边框：`1px solid --border-default`，聚焦时 `2px solid --brand-500`（外加 ring `0 0 0 4px brand-100`）
- 错误态：边框 `expense-500`，下方错误文案 `expense-500 / fs-caption`
- 占位符：`--text-tertiary`
- 必填：标签后红色 `*`

### 7.3 卡片（Card）
- bg=`--bg-elevated`
- 圆角：`--radius-lg`（12）
- 内边距：`24`
- 阴影：`--shadow-sm`
- 标题区：底部 1px `--divider`，padding-bottom=16
- 可点击态 hover：`--shadow-md`

### 7.4 表格（Table）
- 表头：`bg=neutral-50 / text-secondary / fw=500`，`h=44`
- 行高：`56`
- 行 hover：`bg=neutral-50`
- 斑马纹：可选
- 边框：行间 1px `--divider`（非全边框）
- 金额列：右对齐、等宽数字
- 分页：底部右侧，`size: 20/50/100`

### 7.5 弹窗（Dialog / Modal）
- 居中定位；遮罩 `bg-overlay`
- 圆角：`--radius-lg`
- 阴影：`--shadow-lg`
- 最大宽度：`560/800/960`
- 头部：标题 + 关闭按钮（× 24px），底部 1px `--divider`
- 正文：`padding=24`
- 底部：操作区右对齐，`gap=12`，主按钮在右

### 7.6 导航 / 侧边栏
- 侧边栏：`bg=bg-elevated`，宽 `240`
- 菜单项：`h=44`，`px=16`，圆角 `--radius-md`
- 激活态：`bg=brand-50, text=brand-600`
- hover：`bg=neutral-50`
- 图标：`20×20`，文字：`fs-body`

### 7.7 顶部栏
- `h=64`，底部 1px `--divider`
- 左侧：面包屑 / 当前账本切换器
- 右侧：通知 / 用户头像下拉

### 7.8 空/加载/错误态
- **空态**：居中插画（120×120 SVG 灰阶）+ 文字 + 主操作按钮
- **加载态**：骨架屏（`--n-100` 渐变 shimmer），关键数字用 `--fs-mono` 占位
- **错误态**：
  - 行内错误：表单字段下方 `expense-500 / fs-caption`
  - 整页错误：`bg=expense-100` 横条 + 重试按钮
  - Toast：`bg=expense-500 / 白字 / 4s 自动消失`

### 7.9 Tag / 徽标
- 默认：`bg=neutral-100, text=secondary, radius=xs, h=22, px=8`
- 收入：`bg=income-100, text=income-500`
- 支出：`bg=expense-100, text=expense-500`
- 预警：`bg=warn-100, text=warn-500`

---

## 8. 图标系统

- 库：`Lucide Icons`（线性 1.5px 描边）
- 默认尺寸：`20×20`
- 在导航/按钮中：`16~20`
- 颜色：继承 `currentColor`

---

## 9. 动效

- 过渡：`150ms ease-out`（颜色/阴影），`250ms ease-out`（位移/缩放）
- 弹窗：`200ms fade-in + 8px translateY(-8px) → 0`
- 路由切换：淡入 `150ms`
- 减少动画：`prefers-reduced-motion: reduce` 时禁用所有非必要动画

---

## 10. 可访问性（A11y）

- 文字与背景对比度 ≥ `4.5:1`（WCAG AA）
- 所有交互元素必须有焦点环
- 表单必填字段有 `aria-required="true"`
- 错误信息 `aria-describedby` 关联
- 键盘 Tab 顺序 = 视觉顺序
- 弹窗 focus trap + Esc 关闭
- 颜色不作为唯一信息载体（图标/文字配合）

---

## 11. 响应式断点

| 断点 | 宽度 | 适配 |
|---|---|---|
| `sm` | < 640 | 移动端（次要适配） |
| `md` | 640~1024 | 平板（侧边栏折叠为顶部） |
| `lg` | 1024~1280 | 标准桌面 |
| `xl` | ≥ 1280 | **主要目标** |

> 本期主推 PC 桌面端，移动端在二期。

---

## 12. 主题切换

- 浅色为默认；右上角 `主题切换` 按钮
- 切换走 `data-theme="light|dark"` 挂载在 `<html>`
- 颜色变量全部用 `var(--xxx)`；通过 `data-theme` 覆盖
- 持久化：`localStorage.theme`

---

## 13. 不要做（Don't）

- ❌ 不要使用渐变背景（仅允许品牌主色按钮）
- ❌ 不要使用过多阴影堆叠
- ❌ 不要在数字上用非等宽字体
- ❌ 不要超过 3 级信息层级
- ❌ 不要用红色表示所有强调
- ❌ 不要使用 emoji 作图标（分类图标除外）
