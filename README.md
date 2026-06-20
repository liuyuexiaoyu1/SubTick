# SubTick

[![License](https://img.shields.io/github/license/Fallen-Breath/fabric-mod-template.svg)](http://www.gnu.org/licenses/lgpl-3.0.html)

[English](README_EN.md) | [**中文**](README.md)

一个 Carpet 扩展模组，允许你将服务器冻结在任意刻阶段，并逐阶段单步执行，也支持逐个单步执行方块刻、流体刻、方块事件、实体和方块实体。在客户端安装可获得高亮显示和 HUD。

<img src=https://github.com/lntricate1/SubTick/assets/29168747/40edd5f1-948e-45a0-80a8-06ac7b4e6deb width="600">

## 命令

*`[]` 表示可选参数，`<>` 表示必填参数。参数写法如 `count=1` 表示默认值为 `1`。*

- `tick freeze [phase=subtickDefaultPhase]`：在 `phase` 阶段前冻结/解冻服务器。
- `tick step [count=1] [phase=subtickDefaultPhase]`：步进 `count` 刻，结束于 `phase` 阶段前。支持 `tick step 0 [phase]` 在同一刻内前进到后续阶段。
- `phaseStep [count=1]`：向前步进 `count` 个阶段，**必要时进入下一刻**。
- `phaseStep <phase>`：步进到 `phase` 阶段，**在当前刻内**。
- `phaseStep <phase> force`：步进到下一个 `phase` 阶段，**必要时进入下一刻**。
- `queueStep <queue> [count=1] [range=subtickDefaultRange]`：步进 `queue` 中的 `count` 个元素，范围 `range` 格内，**在当前刻内**。设置 `range` 为 `-1` 取消范围限制。
- `queueStep <queue> [count=1] [range=subtickDefaultRange] force`：同上，但**必要时进入下一刻**。

### 特殊模式

方块事件和方块刻可选择不同的步进模式。方块事件可按深度步进，方块刻可按优先级步进。

- `queueStep blockEvent [mode=index] [count=1] [range=subtickDefaultRange] [force]`
- `queueStep blockTick [mode=index] [count=1] [range=subtickDefaultRange] [force]`

## 客户端配置

通过 ModMenu 打开配置界面。

<img src=https://github.com/lntricate1/SubTick/assets/29168747/9da7e81e-b24e-4dd2-91ee-dc53a92552e4 width=500>
<img src=https://github.com/liuyuexiaoyu1/SubTick/assets/29168747/57d667cd-f2fa-4d19-a441-bfca97eaddf8 width=500>

### 显示

| 配置项      | 说明                                |
|----------|-----------------------------------|
| 显示 HUD   | 控制是否显示刻阶段 HUD                     |
| HUD 对齐   | HUD 对齐到屏幕的哪个边缘或角落                 |
| HUD 偏移 X | HUD 的水平偏移像素                       |
| HUD 偏移 Y | HUD 的垂直偏移像素                       |
| 最大队列显示数  | HUD 中显示的队列元素最大数量                  |
| 最大高亮显示数  | HUD 中高亮元素的最大数量。当队列超过显示大小时用于控制高亮数量 |

### 渲染

| 配置项   | 说明                                        |
|-------|-------------------------------------------|
| 实验性渲染 | 使用新的渲染管线渲染方块高亮（可能存在一些渲染错误）。关闭时使用纯色立方体叠加渲染 |

### 颜色配置

| 配置项    | 说明               |
|--------|------------------|
| 已执行背景色 | 已步进通过的内容的背景颜色    |
| 已执行渲染色 | 已步进通过的内容的高亮渲染颜色  |
| 已执行文字色 | 已步进通过的内容的文字颜色    |
| 已执行深度色 | 已步进通过的内容的深度文字颜色  |
| 执行中背景色 | 正在步进的内容的背景颜色     |
| 执行中渲染色 | 正在步进的内容的高亮渲染颜色   |
| 执行中文字色 | 正在步进的内容的文字颜色     |
| 执行中深度色 | 正在步进的内容的深度文字颜色   |
| 待执行背景色 | 尚未步进的内容的背景颜色     |
| 待执行渲染色 | 尚未步进的内容的高亮渲染颜色   |
| 待执行文字色 | 尚未步进的内容的文字颜色     |
| 待执行深度色 | 尚未步进的内容的深度文字颜色   |
| 新任务背景色 | 新排入队列的内容的背景颜色    |
| 新任务渲染色 | 新排入队列的内容的高亮渲染颜色  |
| 新任务文字色 | 新排入队列的内容的文字颜色    |
| 新任务深度色 | 新排入队列的内容的深度文字颜色  |
| 分隔线色   | HUD 表格中分隔元素的颜色   |
| 位置指示色  | 当前刻阶段位置的箭头和指示线颜色 |

## Carpet 规则

本模组使用 Carpet 规则配置选项。关于文本格式的用法，请参见 [Auxiliary.md](https://github.com/gnembon/fabric-carpet/blob/master/docs/scarpet/api/Auxiliary.md) 中的 `format(components, ...)`。

- `subtickDefaultPhase=blockTick`：默认冻结和步进的刻阶段（命令中未指定时使用）。
- `subtickDefaultRange=32`：queueStep 的默认范围。
- `subtickTextFormat=ig`：命令反馈文本的格式。
- `subtickNumberFormat=iy`：命令反馈数字的格式。
- `subtickPhaseFormat=it`：命令反馈阶段的格式。
- `subtickDimensionFormat=im`：命令反馈维度的格式。
- `subtickErrorFormat=ir`：命令反馈错误的格式。
