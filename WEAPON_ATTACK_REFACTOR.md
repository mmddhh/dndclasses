# 武器攻击结算重构（WeaponAttack.resolve）

> 目标读者：接手实现的 agent / 后续维护者。
> 目的：把"一次武器攻击算出多少伤害"从实时输入流程里抽成**纯函数**，让三条调用路径共用同一套数学：
> 1. 现有实时战斗（`DndCombat.attack`）
> 2. BetterCombat 适配层（`LivingHurtEvent`）
> 3. 未来的回合制系统（dndturn 迁移）

---

## 1. 现状

当前所有结算都写死在 `weapon/DndCombat.java#attack(Player, Entity, InteractionHand, boolean)`（约 32–103 行），职责混在一起：

| 步骤 | 位置 | 是否应该留在"纯函数"里 |
|---|---|---|
| 客户端/目标类型检查 | `attack` 开头 | 否 |
| 攻击冷却 `DndCooldowns.tryUse` | `attack` 第 37 行 | 否（实时输入专属） |
| 双手可用性 `canUseWeapon` | `attack` 第 48 行 | 否（可返回"拒绝原因"） |
| 武器/工具/空手/违禁品分支 | `attack` 45–78 行 | 是（算基础伤害） |
| 基础伤害 `spec.damage(offhandEmpty)` | 第 56 行 | 是 |
| 属性调整值 `DndAttributes.modifier` | 第 57 行 | 是 |
| 暴击判定（5% + 精金对构装） | 第 58–59 行 | 是（需要随机源） |
| 双手加成（versatile + 副手为空） | 第 55–56 行 | 是 |
| 破损武器最低伤害 | 第 50–51 行 | 是 |
| 特性加成（`FeatureEffects.attackBonus` / `extraAttacks`） | 第 81–92 行 | 是（作为输入或内部读取） |
| 伤害类型 | 第 61 行 | 是 |
| 耐久消耗 | 第 63–65 行 | 否（返回标记，由调用方决定） |
| `hurt` / 粒子 / `swing` / 冷却重置 | 第 80–101 行 | 否 |

---

## 2. 目标 API

放在 `weapon/`（核心包，**不得**引用 BetterCombat 或客户端类）。

```java
package mdh.dndclasses.weapon;

/** 一次攻击的输入。所有可变状态由调用方准备好。 */
public record AttackInput(
        ServerPlayer attacker,
        LivingEntity target,
        ItemStack stack,
        InteractionHand hand,
        boolean rangedAsMelee,     // 用远程武器近战
        boolean offHand,           // 是否副手攻击（双持/副手近战）
        int bonusDamage,           // FeatureEffects.attackBonus
        int extraAttacks           // FeatureEffects.extraAttacks（副手为 0）
) {}

/** 结算结果。纯数据，不产生副作用。 */
public record AttackResult(
        boolean allowed,
        DenyReason denyReason,     // allowed=false 时给出原因
        float amount,
        DndDamageType damageType,
        boolean crit,
        boolean consumeDurability
) {
    public enum DenyReason { NONE, TWO_HANDED_VIOLATION, FORBIDDEN_ITEM }
}

public final class WeaponAttack {
    /** 纯函数：不读冷却、不写耐久、不发消息、不触发 hurt。 */
    public static AttackResult resolve(AttackInput input, RandomSource random) { ... }
}
```

约定：

- **纯函数**：除 `random` 外不产生副作用；同一输入同一随机序列 → 同一输出。
- 需要随机的地方只有暴击，`RandomSource` 由调用方传入（实时用 `player.getRandom()`，回合制可注入固定种子便于回放/测试）。
- 拒绝原因用枚举表达，文案由调用方翻译（`message.dndclasses.two_handed` / `disabled_item`）。

---

## 3. 结算算法（伪代码）

```
resolve(input):
  player, target, stack, hand = input

  # 1. 道具分类
  if stack is DndWeapon weapon:
      spec = weapon.spec()
      broken = DndDurability.isBroken(stack)

      # 1a. 双手规则（仅实时/BB 需要；turn 可自行判断）
      if !broken && !canUseWeapon(player, hand, spec):
          return denied(TWO_HANDED_VIOLATION)

      # 1b. 基础伤害
      if broken:
          base = minimumDamage(1 + strengthModifier(player))
          damageType = BLUDGEONING
          crit = false
      else if input.rangedAsMelee && spec.isRanged():
          base = minimumDamage(2 + strengthModifier(player))
          damageType = spec.damageType()
          crit = false
      else:
          offhandEmpty = player.getOffhandItem().isEmpty()   # 注意：BB 下该值仍读真实副手
          base = spec.damage(offhandEmpty)                   # versatile 双手值在此生效
          damageType = spec.damageType()
          ability = DndAttributes.modifier(player, spec.abilityMode())
          crit = random.nextFloat() < 0.05
                 || (spec.material() == ORICHALCUM && isConstruct(target))
          amount = crit ? base * 2 + ability : base + ability
          return allowed(amount + bonusDamage) * (1 + extraAttacks), damageType, crit, !broken

  else if VanillaEquipmentRules.isAllowedTool(stack):
      base = minimumDamage(2 + strengthModifier(player))
      damageType = toolDamageType(stack)
      crit = random.nextFloat() < 0.05
      return allowed((base + bonusDamage) * (1 + extraAttacks)), damageType, crit, true

  else if stack.isEmpty():
      base = minimumDamage(1 + strengthModifier(player))
      crit = random.nextFloat() < 0.05
      return allowed((base + bonusDamage) * (1 + extraAttacks)), BLUDGEONING, crit, false

  else if VanillaEquipmentRules.isForbidden(stack):
      return denied(FORBIDDEN_ITEM)

  else:
      base = minimumDamage(1 + strengthModifier(player))
      crit = random.nextFloat() < 0.05
      return allowed((base + bonusDamage) * (1 + extraAttacks)), BLUDGEONING, crit, false
```

要点：

1. **`bonusDamage` / `extraAttacks` 由调用方传入**，而不是在函数里读 `FeatureEffects`：
   - 实时路径：`FeatureEffects.attackBonus(player)` / `extraAttacks(player)`；
   - 回合制：由动作经济系统决定这一击吃不吃加值/额外攻击；
   - 副手：`extraAttacks` 必须为 0（D&D 附赠攻击不享受额外攻击），`bonusDamage` 保留（如狂暴加伤）。
2. **副手属性调整值**：D&D 规则为"不加正调整值，负值照加"。建议在调用方处理：
   ```java
   int ability = DndAttributes.modifier(player, spec.abilityMode());
   if (offHand) ability = Math.min(ability, 0);
   ```
   如果希望函数内部统一处理，可把 `offHand` 传入并在 `ability` 计算处封顶（推荐，避免调用方漏掉）。
3. **`offhandEmpty` 的语义**：`spec.damage(true)` 表示"双手握持值"。BB 的 `getEquippedStack(OFFHAND)` 会伪造空槽，但 `Player#getOffhandItem()` **不受影响**（它直接读 inventory），所以这里读到的仍是真实副手。这符合 D&D：有副手物品就不能双手握持（versatile 武器）。
4. **暴击**：当前是"5% 或 精金武器对构装"，与命中无关（本模组无命中骰）。`crit` 只影响伤害与粒子。
5. **`minimumDamage`** 保持 `Math.max(1, damage)`。
6. **伤害类型**：`DndWeaponSpec.damageType()`；工具用 `VanillaEquipmentRules.toolDamageType`；空手/违禁品用 BLUDGEONING。
7. **耐久**：返回 `consumeDurability`，由实时路径 `stack.hurtAndBreak(...)`；回合制可自行决定（例如忽略耐久）。

---

## 4. 调用方改造

### 4.1 实时路径（`DndCombat.attack`）

```java
public static boolean attack(Player player, Entity target, InteractionHand hand, boolean rangedAsMelee) {
    if (player.level().isClientSide || !(target instanceof LivingEntity living)) return false;
    if (!DndCooldowns.tryUse(player, hand)) return true;
    if (!(player instanceof ServerPlayer serverPlayer)) return false;

    ItemStack stack = player.getItemInHand(hand);
    AttackInput input = new AttackInput(serverPlayer, living, stack, hand, rangedAsMelee,
            hand == InteractionHand.OFF_HAND,
            (int) Math.round(FeatureEffects.attackBonus(serverPlayer)),
            hand == InteractionHand.OFF_HAND ? 0 : FeatureEffects.extraAttacks(serverPlayer));

    AttackResult result = WeaponAttack.resolve(input, player.getRandom());
    if (!result.allowed()) {
        // 翻译 denyReason，必要时发消息
        return true;
    }
    if (result.consumeDurability()) stack.hurtAndBreak(1, player, e -> e.broadcastBreakEvent(hand));
    living.hurt(ModDamageTypes.source(player.level(), result.damageType(), player, player), result.amount());
    if (result.crit()) spawn CritParticles;
    player.swing(hand, true);
    player.resetAttackStrengthTicker();
    return true;
}
```

### 4.2 BetterCombat 适配层（`compat/bettercombat`）

BB 路径下 `DndCombat.attack` 不接管，改为在 `LivingHurtEvent` 里**用 `resolve` 重算**并 `setAmount`：

```java
AttackInput input = new AttackInput(player, (LivingEntity) event.getEntity(), stack,
        mainOrOffHand(player), false, isOffHand(player),
        (int) Math.round(FeatureEffects.attackBonus(player)),
        isOffHand(player) ? 0 : FeatureEffects.extraAttacks(player));
AttackResult result = WeaponAttack.resolve(input, player.getRandom());
event.setAmount(result.amount());
```

> 之所以可以直接覆盖 `event.getAmount()`：已确认 BB 的连招倍率全部为 1、双持伤害倍率为 1、横扫衰减已关闭，所以 BB 的原始数值就是武器基础值，我们的 `resolve` 才是唯一权威。

### 4.3 回合制（dndturn 迁移后）

```java
AttackResult result = WeaponAttack.resolve(input, random);
target.hurt(customSource(result.damageType()), result.amount());
```

动作经济（攻击/附赠/反应、每回合攻击次数、双持合法性）由 turn 层决定，`resolve` 只负责"这一击的数值"。

---

## 5. 边界情况清单

| 情况 | 期望行为 |
|---|---|
| 破损武器 | 1 + 力量调整值，钝击，无暴击，不消耗耐久 |
| 远程武器近战（`rangedAsMelee`） | 2 + 力量调整值，武器伤害类型，无暴击 |
| 长矛/骑枪（`requiresTwoHands`） | 副手非空时 `TWO_HANDED_VIOLATION`（骑枪仅下马时） |
| 违禁物品（`VanillaEquipmentRules.isForbidden`） | `FORBIDDEN_ITEM` |
| 允许的vanilla工具 | 2 + 力量调整值 + 工具伤害类型，消耗耐久 |
| 空手 | 1 + 力量调整值 |
| 捕网（`net`，伤害 0） | `minimumDamage` 兜底为 1 |
| 精金武器打构装 | 必暴击 |
| 副手攻击 | 不享受额外攻击；属性调整值封顶 0 |
| 事件/特性加成来源 | 只通过 `AttackInput` 传入，函数内不读 capability |

---

## 6. 测试矩阵

1. 单测（无 MC 依赖的部分）：`minimumDamage`、暴击公式、双手加值、副手封顶。
2. 游戏内（无 BB）：铁长剑 = 4 + 力量；巨剑双手 = 7 + 力量；破损 = 1 + 力量；长矛副手有物品 → 拒绝。
3. 游戏内（有 BB）：同一套数值（对比 4.2 覆盖后与 4.1 一致）。
4. 回合制模拟：固定随机种子，同一输入输出稳定。

---

## 7. 非目标

- 不引入命中骰（本模组无 to-hit；若未来要做，作为独立层 `HitRoll` 加在 `resolve` 之前）。
- 不处理冷却/动作经济（属于调用方）。
- 不改动 `DndWeaponSpec` 数据模型。
- 不在本重构中处理 BB 的双持合法性（见 `BB_INTEGRATION.md`）。

---

## 8. 迁移提示（dndturn）

- 本文件涉及的 `weapon/`、`armor/`、`level/`、`feature/` 等核心包**不得**出现 `net.bettercombat.*` 引用。
- 迁移时 `compat/bettercombat/` 与 `data/dndclasses/weapon_attributes/` 整个不带走。
- 若 turn 层需要"一次攻击的完整过程"（含动作消耗、命中、豁免），建议在其之上再包一层 `TurnAttackResolver`，内部调用 `WeaponAttack.resolve`。
