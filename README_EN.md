# SubTick

[![License](https://img.shields.io/github/license/Fallen-Breath/fabric-mod-template.svg)](http://www.gnu.org/licenses/lgpl-3.0.html)

[**English**](README_EN.md) | [中文](README.md)

A Carpet extension that allows you to freeze and step to any specific tick phase, as well as step through block ticks, fluid ticks, block events, entities, and block entities individually. Get it on your client for highlights and a HUD.

<img src=https://github.com/lntricate1/SubTick/assets/29168747/40edd5f1-948e-45a0-80a8-06ac7b4e6deb width="600">

## Commands

*[] represents an optional argument, and <> represents an obligatory argument. If an argument is written like `count=1`, that means `1` is the default value.*

- `tick freeze [phase=subtickDefaultPhase]`: Freezes/unfreezes right before `phase`.
- `tick step [count=1] [phase=subtickDefaultPhase]`: Steps `count` ticks, ending right before `phase`. Supports `tick step 0 [phase]` to step to a later phase in the same tick.
- `phaseStep [count=1]`: Steps `count` phases forward, **stepping to the next tick if necessary**.
- `phaseStep <phase>`: Steps to `phase`, **within the current tick**.
- `phaseStep <phase> force`: Steps to the next `phase` **stepping to the next tick if necessary**.
- `queueStep <queue> [count=1] [range=subtickDefaultRange]`: Steps through `count` elements in `queue` within `range` blocks, **within the current tick**. Set `range` to `-1` for unlimited range.
- `queueStep <queue> [count=1] [range=subtickDefaultRange] force`: Steps through `count` elements in `queue` within `range` blocks, **stepping to the next tick if necessary**. Set `range` to `-1` for unlimited range.

### Special modes

Block events and block ticks have the option to use a different mode for stepping. Block events can step through whole block event depths, and block ticks can step through whole block tick priorities.

- `queueStep blockEvent [mode=index] [count=1] [range=subtickDefaultRange] [force]`
- `queueStep blockTick [mode=index] [count=1] [range=subtickDefaultRange] [force]`

## Client config

Open the config screen via ModMenu.

<img src=https://github.com/lntricate1/SubTick/assets/29168747/9da7e81e-b24e-4dd2-91ee-dc53a92552e4 width=500>
<img src=https://github.com/lntricate1/SubTick/assets/29168747/57d667cd-f2fa-4d19-a441-bfca97eaddf8 width=500>

### Display

| Option | Description |
|--------|-------------|
| Show HUD | Controls whether the HUD is shown |
| HUD Alignment | Which edge or corner of the screen the HUD is aligned to |
| HUD Offset X | Horizontal pixel offset of the HUD |
| HUD Offset Y | Vertical pixel offset of the HUD |
| Max Queue Size | Maximum number of queue elements displayed in the HUD |
| Max Highlight Size | Maximum number of highlighted queue elements in the HUD. Useful when the queue exceeds the display size |

### Rendering

| Option | Description |
|--------|-------------|
| Experimental Rendering | Use the new rendering pipeline for block highlights (may have rendering bugs). When disabled, uses solid colored cube overlay |

### Colors

| Option | Description |
|--------|-------------|
| Stepped Background | Background color for things already stepped through |
| Stepped Render | The highlight render color for things already stepped through |
| Stepped Text | Text color for things already stepped through |
| Stepped Depth | Text color for depth of things already stepped through |
| Stepping Background | Background color for things being stepped through |
| Stepping Render | The highlight render color for things being stepped through |
| Stepping Text | Text color for things being stepped through |
| Stepping Depth | Text color for depth of things being stepped through |
| To Step Background | Background color for things not stepped through |
| To Step Render | The highlight render color for things not stepped through |
| To Step Text | Text color for things not stepped through |
| To Step Depth | Text color for depth of things not stepped through |
| New Background | Background color for newly scheduled things |
| New Render | The highlight render color for newly scheduled things |
| New Text | Text color for newly scheduled things |
| New Depth | Text color for depth of newly scheduled things |
| Separator | Color for separating elements in the HUD table |
| Position | Color for the arrow and line indicating current position in the tick |

## Carpet rules

This mod uses Carpet rules for its configuration options. For how to use the text formatting, search for "`format(components, ...)`" in [Auxiliary.md](https://github.com/gnembon/fabric-carpet/blob/master/docs/scarpet/api/Auxiliary.md).

- `subtickDefaultPhase=blockTick`: The default tick phase to freeze at and step to, if not specified in the command.
- `subtickDefaultRange=32`: The default range for queueStep.
- `subtickTextFormat=ig`: The format for command feedback text.
- `subtickNumberFormat=iy`: The format for command feedback numbers.
- `subtickPhaseFormat=it`: The format for command feedback phases.
- `subtickDimensionFormat=im`: The format for command feedback dimensions.
- `subtickErrorFormat=ir`: The format for command feedback errors.
