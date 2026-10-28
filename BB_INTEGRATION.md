# Better Combat 软兼容说明

状态：**软兼容**。Better Combat（下称 BB）是**可选**依赖（`mods.toml` 中 `mandatory = false`）。不安装时走本模组自研战斗；安装时 BB 只承担表现层与命中几何，D&D 数值仍由本模组决定。

## 1. 合规边界

本项目**不引用任何 `net.bettercombat.*` 类**，也不对 BB 的类做 mixin 或运行时配置覆盖。允许的交互面只有：

| 交互面 | 用法 |
|---|---|
| 依赖声明 | `mods.toml` 可选依赖 + 运行时 `ModList.isLoaded("bettercombat")` |
| 数据文件 | `data/dndclasses/weapon_attributes/*.json`（引用 `bettercombat:*` 动画 id，不复制资源） |
| 公共 API | `net.bettercombat.api.EntityPlayer_BetterCombat#getCurrentAttack`（反射调用，保持零编译依赖） |
| Forge 事件 | `LivingHurtEvent`（改数值） |
| 原版 mixin | `LivingEntity#hurt`（改伤害类型），与 BB 无关 |

BB 的 README 把“双持实现 / 攻击距离 / 攻击时序 / 攻击键处理 / 玩家模型修改”列为**语义不兼容**，因此本模组在 BB 模式下**主动让出**这些决策：不接管 `AttackEntityEvent`、不拦 `swing`、不自行结算命中。

## 2. 模式切换

- `compat/BetterCombatCompat.isActive()`：`ModList.get().isLoaded("bettercombat")`，结果缓存。
- 为 true 时：
  - `DndCombatEvents.onAttackEntity` 直接返回（不取消事件，BB/原版继续）；
  - `LivingEntitySwingMixin` 不再取消客户端挥击；
  - 由 `compat/BetterCombatDamageHandler` 在 `LivingHurtEvent` 中接管数值。
- 为 false 时：完全走 `DndCombat.attack`，与旧版本行为一致。

## 3. 职责划分

| 能力 | 原版环境 | BB 模式 |
|---|---|---|
| 攻击时序 / 冷却 | `DndCooldowns`（10 tick，双持 5 tick） | BB（建议配置为 2.0 倍速，见 §5） |
| 命中几何 / 距离 | 原版攻击 + `ENTITY_REACH` | BB 的 `attack_range` / 命中盒 |
| 动画 | 原版 | BB |
| 伤害数值 | `DndCombat.attack` | `BetterCombatDamageHandler` |
| 伤害类型 | `ModDamageTypes.source` | `LivingEntityHurtTypeMixin` |
| 双持合法性 | `DualWieldRules` / `DualWieldEvents` | 同一套（装备准入先行保证） |
| 右键 | 副手轻型投掷 / 弩装填 / 发射 | 同左（BB 不处理 `Item.use`） |

## 4. 数值覆盖

`BetterCombatDamageHandler` 在 BB 每次结算时**整体覆盖** `LivingHurtEvent#setAmount`：

- BB 的连招倍率（`damage_multiplier`）、双持伤害倍率、横扫衰减最终都表现为攻击力属性/事件数值的调整；
- 覆盖后这些全局倍率**不会**进入游戏数值，因此：
  - 连招倍率可以全部写 `1`（默认即可）；
  - 不需要修改 BB 的 `reworked_sweeping_*`；
  - 不需要修改 `dual_wielding_*_damage_multiplier`。

覆盖公式与 `DndCombat.attack` 保持一致（见 `WEAPON_ATTACK_REFACTOR.md`，后续重构会抽出 `WeaponAttack.resolve` 供两边共用）：

```
damage = 武器骰（破损/远程近战/双手特殊处理）
       + 属性调整值（副手封顶为 0）
       + 特性攻击加值
       × (1 + 额外攻击)   // 副手为 1
       × 暴击（2 倍）
```

## 5. 需要的服务器配置（整合包处理）

BB 的 server config 位于 `config/bettercombat/server.json5`。本模组不强制修改，建议：

| 配置 | 建议值 | 原因 |
|---|---|---|
| `dual_wielding_attack_speed_multiplier` | `2.0` | 与本模组双持 5 tick 的节奏对齐 |
| `movement_speed_while_attacking` | `1.0` | 取消攻击减速（本模组有体力/翻滚节奏） |
| `fallback_compatibility_enabled` | `false` | 防止 BB 给原版工具自动分配武器预设，绕过装备规则 |
| `allow_attacking_thru_walls` | `false` | 保持原版手感 |

伤害类倍率无需处理（见 §4）。

## 6. 伤害类型

`mixin/LivingEntityHurtTypeMixin` 在 `LivingEntity#hurt` 的 `HEAD` 替换 `DamageSource`：

- 仅当攻击者是 `ServerPlayer`、`directEntity == player`、且手持 `DndWeapon`；
- 替换为目标武器的 `DndDamageType`；
- 我方自研路径传入的已经是该类型，重复替换无副作用；
- 弹射物（箭/投掷武器）的 `directEntity` 是投射物，不受影响。

这样 BB 模式下挥砍/穿刺/钝击标签、抗性与构装判定仍然生效。

## 7. 双持与装备准入

BB 的 `isDualWielding` 只检查“双手都有非双手武器属性”，不检查轻型。为了不改 BB 且避免“非法组合交替挥副手产生空挥”，本模组把规则前移到装备层（`DualWieldRules`/`DualWieldEvents`）：

- 副手放武器要求主手是轻型武器；
- 因此任何时刻存在的双持组合都满足“双手轻型”，BB 的交替只会发生在合法组合上。

## 8. 数据文件（待补）

`data/dndclasses/weapon_attributes/<物品>.json` 尚未生成（每种材质一份，共 95 个）。建议结构：

```json
{
  "parent": "bettercombat:sword",
  "attributes": {
    "category": "longsword",
    "attacks": [
      { "damage_multiplier": 1 },
      { "damage_multiplier": 1 },
      { "damage_multiplier": 1 }
    ]
  }
}
```

- 映射表见 `WEAPON_ATTACK_REFACTOR.md` 讨论记录；
- 鞭 / 捕网 / 吹箭筒 / 投石索 / 飞镖没有现成预设，需要自定义动画或临时复用 `fist` / `staff`。

## 9. 已知限制

- BB 模式下无法阻止 BB 播放副手挥击动画（数值与合法性正确）。
- BB 的 `getEquippedStack(OFFHAND)` 会在双手武器时返回空栈；本模组读取 `getOffhandItem()`，不受影响。
- 原版工具在 BB 模式下可能被 fallback 自动分配预设；关闭 `fallback_compatibility_enabled` 可避免。

## 10. 迁移到 turn

- 带走：`weapon/`、`armor/`、`level/`、`feature/` 等核心包；
- 不带走：`compat/bettercombat`（即 `BetterCombatCompat`、`BetterCombatDamageHandler`）、`data/dndclasses/weapon_attributes/`、`mods.toml` 的 `bettercombat` 依赖；
- 装备规则（`DualWieldRules`/`DualWieldEvents`）属于核心，保留。
