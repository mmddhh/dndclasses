# 双持战斗（Two-Weapon Fighting）

本文描述 dndclasses 在**原版环境**（未安装 Better Combat）下的双持实现，以及安装 Better Combat 时的对应关系。

## 1. 规则

### 1.1 装备准入

| 主手 | 副手放 `DndWeapon` | 结果 |
|---|---|---|
| 空 | 允许 | 只持有，不构成双持 |
| 非武器物品（方块/火把/工具/盾） | 允许 | 只持有，不构成双持 |
| **轻型** `DndWeapon` | 允许 | 构成双持 |
| **非轻型** `DndWeapon` | **拒绝** | 副手物品被收回背包（背包满则掉落）+ 提示 |

- 主手切换到非轻型武器时，若副手持有武器 → 副手武器自动收回背包（`message.dndclasses.offhand_light_only`）。
- 死亡、换维度、创造模式切换不做回收，避免吞物品。
- 实现：`weapon/DualWieldRules`、`weapon/DualWieldEvents`（`LivingEquipmentChangeEvent`）。

### 1.2 双持判定

主手、副手同时满足：

1. 都是 `DndWeapon`；
2. 都带 `WeaponProperty.LIGHT`；
3. 不是同一个 `ItemStack`（Minecraft 中两手持同一堆叠不可能，仅作防御）。

### 1.3 “另一把武器”

D&D 要求 "a different light melee weapon"——指另一个**武器物体**，不是另一种类型。两把同型号匕首合法；Minecraft 主副手本来就是两个堆叠，所以天然满足。

## 2. 出手规则（原版环境）

- 左键攻击按**主手 → 副手 → 主手**交替，**每 10 tick 两次攻击**：
  - 单手：冷却 10 tick（`DndCooldowns.ATTACK_COOLDOWN_TICKS`）；
  - 双持：冷却 5 tick（`DUAL_WIELD_COOLDOWN_TICKS`），交替出手中和，相当于主手每 10 tick 一次 + 副手每 10 tick 一次。
- 冷却为**单手共用**（`DndWeaponSwingCooldown`），交替本身不缩短间隔。
- 30 tick 未攻击 → 重置为主手起手（`DualWieldRules.RESET_TICKS`）。
- 装备变化 / 死亡 / 退出 → 重置。

### 2.1 伤害

| 项 | 主手 | 副手 |
|---|---|---|
| 武器骰 | `spec.damage(offhandEmpty)` | 同左（副手占用手，所以不会触发双手加成） |
| 属性调整值 | 完整 | **正数不生效**，负数照加（`min(modifier, 0)`） |
| 额外攻击 | 生效 | **不生效** |
| 攻击加值（狂暴等） | 生效 | 生效 |
| 暴击 | 5% / 精金对构装 | 同左 |
| 伤害类型 | 武器类型 | 同左 |

### 2.2 右键

- 右键**只用于副手投掷**：副手是带 `THROWN` 且带 `LIGHT` 的武器时投掷（当前为匕首、轻锤、手斧）。
- 右键副手近战攻击已移除（近战由左键交替触发）。
- 标枪、矛、三叉戟不是轻型武器，不能放副手，只能在主手投掷。

## 3. 与 Better Combat 的对应

| | 原版环境 | 安装 Better Combat |
|---|---|---|
| 出手节奏 | 我们的交替，双持 5 tick | BB 的交替，`dual_wielding_attack_speed_multiplier = 2.0` 时为 5 tick |
| 手别判定 | `DualWieldRules.nextHand` | BB 的 `comboCount`（`BetterCombatCompat.isOffHandAttack` 反射读取） |
| 伤害 | `DndCombat.attack` | `BetterCombatDamageHandler`（`LivingHurtEvent` 覆盖数值） |
| 伤害类型 | `ModDamageTypes.source` | `LivingEntityHurtTypeMixin` 替换 |
| 装备准入 | 相同 | 相同（BB 下也生效，保证 BB 不会遇到非法双持） |

**整合包配置建议**：`config/bettercombat/server.json5` 里

- `dual_wielding_attack_speed_multiplier: 2.0` —— 与原版环境的 5 tick 对齐；
- `movement_speed_while_attacking: 1.0` —— 取消攻击减速（可选）；
- `fallback_compatibility_enabled: false` —— 避免 BB 自动给原版工具分配预设、绕过装备规则。

## 4. 已知偏差

- 没有动作经济（每回合一次附赠动作）：实时环境下通过“每 10 tick 一次副手攻击”近似。
- 主手必须是轻型武器是硬规则（D&D RAW），因此长剑/长弓无法与副手武器组成双持。
- BB 模式下无法阻止 BB 播放副手挥击动画（我们只保证数值与合法性）。

## 5. 测试矩阵

1. 主手长剑 + 副手匕首 → 副手匕首被收回，提示。
2. 主手短剑 + 副手匕首 → 左键攻击交替，副手伤害不含力量调整值（+3 力量时副手 = 武器骰）。
3. 负力量调整值 + 副手 → 负值生效。
4. 双持 5 级战士 → 主手吃额外攻击（翻倍），副手不吃。
5. 双持时右键 → 投掷副手匕首；标枪放不进副手。
6. 30 tick 不攻击后再攻击 → 从主手开始。
7. 装备变化 / 死亡 / 重登 → 交替状态重置。
8. 安装 BB：同样规则，伤害数值与原版环境一致；BB 的连招/横扫倍率不影响数值。
