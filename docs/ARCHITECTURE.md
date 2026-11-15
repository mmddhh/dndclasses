# dndClasses 架构边界（审计稿）

状态：只做依赖审计，不移动代码。目标发布形态仍是**一个模组 jar**，内部按模块约束。

## 1. 目标模块

| 模块 | 职责 | 允许依赖 |
|---|---|---|
| `core` | 纯数据/规则：WorldPlan、历史模拟、种族/职业表、校验器 | 无 Minecraft 依赖 |
| `character` | 角色创建、等级、子职、ASI、特性、状态 | `core` |
| `combat` | 武器、护甲、双持、法术、法术位 | `core`、`character` |
| `world` | 聚落、NPC 档案、地形采样、结构放置 | `core` |
| `platform` | Forge 注册、capability、网络、客户端、命令、Mixin、兼容层 | 全部 |

规则：

- `core` 不 import `net.minecraft`，也不 import 其他模块；
- `world` 不 import `combat` / `character`；
- `combat` 不 import `client` / `mixin` / `command`；
- `character` 不 import `combat` 的具体类，只通过 SPI；
- `platform` 是唯一允许 import `net.minecraft.client` 的层。

## 2. 当前事实

### 2.1 好的部分

- `worldgen` 目前**没有任何 `mdh.dndclasses.*` import**，是干净的纯规划层；
- `worldgen` 只依赖 Minecraft 的 `ServerLevel`、`SavedData`（在 `NpcSpawner` / `WorldPlanSavedData` / `SettlementSiteSampler` 中），没有反向依赖战斗/角色；
- `command` 只作为入口 import `worldgen`，方向正确。

### 2.2 需要处理的耦合

| 方向 | 文件示例 | 问题 |
|---|---|---|
| `weapon` → `client` | `weapon/DndCombatEvents.java` import `client/ClientReloadState` | **已修复**：改为 `compat/ClientHooks`，客户端启动时注册实现 |
| `character` ↔ `combat` | `feature` import `armor` / `spellcasting`；`weapon` import `feature` / `ability` | 形成双向依赖，无法物理拆模块 |
| `data` ↔ `network` | `data` import `network`；`network` import `data` | 数据层直接依赖传输层 |
| `capability` 作为枢纽 | 几乎全部包 import `capability` | 可接受，但应集中在 platform 层，内容层尽量用接口 |

## 3. 物理拆分前的约束

1. 所有新代码先判断属于哪个模块，不要继续往 `data`/`feature` 里堆；
2. `worldgen` 保持零 `mdh` import；需要 Minecraft 的适配器放在 `worldgen/mc` 子包，并只被 `platform` 引用；
3. 不再新增 `weapon` → `client`、`feature` → `armor` 这类直接引用；
4. 网络发送、客户端状态、Mixin 一律只在 `platform` 层出现；
5. 跨模块调用用接口（`character/spi`、`combat/spi`）而不是具体类。

## 4. 建议的最小重构顺序

1. **`weapon` → `client`：已完成**
   - `ClientReloadState.clear()` 已改为 `ClientHooks.clearReloadAnimation()`；
   - `ClientHooks` 放在 `compat`，客户端启动时注册实现；
   - 服务端不再 import `net.minecraft.client.*`。

2. **抽 `character/spi`**
   - `ArmorStats`（AC/护甲加成）；
   - `SpellcastingHooks`（法术位/施法属性）；
   - `feature` 依赖接口，不依赖 `armor` / `spellcasting` 具体类。

3. **把 `data` 的网络发送移到 `platform`**
   - `data` 只返回结果对象；
   - `platform` 负责 `DndNetwork.sendToPlayer`。

4. **拆 `worldgen` 的 MC 适配**
   - 纯数据留在 `worldgen`；
   - `NpcSpawner`、`WorldPlanSavedData`、`SettlementSiteSampler` 移到 `worldgen/mc`，只由 platform 调用。

5. 等以上边界稳定，再考虑 Gradle 子项目/独立发布物。

## 5. 不做什么

- 现在不拆成多个玩家需要分别安装的 mod；
- 现在不把 `world` 抽成独立 addon；
- 现在不引入 ArchUnit/额外构建插件，先用包边界和代码评审约束。

## 6. 何时重新评估拆分

满足任意两条再考虑：

- 世界生成成为可选内容；
- 世界生成需要独立发版节奏；
- 外部模组要复用 WorldPlan/NPC API；
- 单 jar 体积或启动成本明显影响玩家；
- 需要专用服务器/客户端裁剪。
