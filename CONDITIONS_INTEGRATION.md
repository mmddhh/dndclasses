# D&D 状态（Conditions）接入说明（dndclasses 侧）

> 给 dndspells / dndturn 的对接文档。条件系统已实现并提交（commit `3a49095` 起）。本文件说明数据、API、行为与调用方式。

## 一、这次新增了什么
- **dndweapon 已合并进 dndclasses**（武器/弹药/伤害类型/战斗规则/装填）。原 `dndweapon:*` 物品 id 全部改为 `dndclasses:*`。
- **新增 D&D 状态系统**（14 个 5e 状态 + 力竭）。
- 网络协议升到 **8**；`ClientboundCharacterStatePacket` 多了一个 `conditionsData`（NBT），并新增 `RollPacket` / `StaminaSyncPacket` / `RollAnimationPacket`。
- 键位：`O`=职业/角色界面，`V`=特性轮盘，`G`=豁免 QTE，`R`=武器装填，`Mouse 4`=翻滚。
- **护甲/体力/矿石**：144 件护甲（3 材质 × 12 类 × 4 部位，整套才给 AC）、体力与翻滚、暴击、耐久损坏 + 铁砧修复、禁用原版护甲；矿石合并（orichalcum/mithril/silver）与 JSON 世界生成。武器材质 `adamantine` 已统一改名为 `orichalcum`（含物品 id/贴图/配方/语言键）。

## 二、条件数据（数据包）
位置：`data/<namespace>/dndclasses/conditions/<id>.json`。内置 14 个，id 为 `dndclasses:<name>`：

`blinded, charmed, deafened, frightened, grappled, incapacitated, invisible, paralyzed, petrified, poisoned, prone, restrained, stunned, unconscious`

Schema：
```jsonc
{
  "id": "dndclasses:petrified",
  "name_key": "condition.dndclasses.petrified",
  "description_key": "condition.dndclasses.petrified.desc",
  "save_ability": "con",          // "" = 不能靠豁免提前结束
  "save_dc": 12,                  // 默认 DC（apply 可覆盖）
  "save_interval_ticks": 80,      // 4 秒一次豁免
  "duration_ticks": 600,          // 默认时长（apply 可覆盖）
  "flags": ["incapacitated","immobile","half_damage"],
  "status_effects": [ { "effect": "minecraft:blindness", "amplifier": 0 } ],
  "attribute_modifiers": [ { "attribute": "minecraft:generic.movement_speed", "amount": -0.5, "operation": "multiply_total" } ]
}
```
可用的 `flags`：`incapacitated, immobile, blind, invisible, prone, unconscious, drop_items, half_damage, weakness, cannot_attack_source, cannot_approach_source`。

## 三、API（server 端）
包：`mdh.dndclasses.conditions`。

```java
// 施加（durationTicks <= 0 用定义默认；dc <= 0 用定义 save_dc；source 可为 null）
ConditionManager.apply(ServerPlayer player, ResourceLocation condition, int durationTicks, Entity source, int dc);

ConditionManager.remove(ServerPlayer player, ResourceLocation condition, DndConditionRemovedEvent.Reason reason);
ConditionManager.has(player, condition);
ConditionManager.getExhaustion(player);
ConditionManager.setExhaustion(player, level);      // 0..6
ConditionManager.snapshot(player);                  // 给 HUD：List<CompoundTag>{id,flags,duration}
```

事件（server，Forge bus）：
- `DndConditionAppliedEvent(ServerPlayer, ResourceLocation, int durationTicks, UUID source)`
- `DndConditionRemovedEvent(ServerPlayer, ResourceLocation, Reason{EXPIRED,SAVE,CURED,DEATH,REPLACED,OTHER})`

**周期豁免**：对 `save_ability` 非空的条件，每 `save_interval_ticks`（默认 80t=4s）服务端掷 `d20 + 该属性总豁免加值 >= dc`，成功即 `remove(SAVE)`，动作栏显示成功/失败。**DC 用 `apply` 传入的值**（建议传施法者法术豁免 DC），缺省用定义里的 `save_dc`。

## 四、dndspells 里怎么用
在 `CastSpellPacket.execute(...)` 命中/命中失败后按法术规则施加：

```java
// 例：法术迫使目标做感知豁免，失败则恐惧 10 分钟，DC=施法者法术豁免DC
int dc = spellSaveDc(player);          // dndspells 自己算：8 + 熟练 + 施法属性调整值
ConditionManager.apply(target, new ResourceLocation("dndclasses", "frightened"),
        20 * 60 * 10, player, dc);
```

注意：
- **魅惑/恐惧/擒抱/束缚**建议传 `source = 施法者`，这样"不能攻击/靠近来源"才会生效。
- 条件自带周期豁免；如果法术是"命中即持续、无豁免"，把条件定义或 `apply` 的 `save_ability` 场景处理好（当前周期豁免由条件 JSON 决定；若需要某次施加**不**豁免，可后续加 `boolean allowSave` 参数，先按 JSON）。
- 施加**失能类**（incapacitated/paralyzed/petrified/stunned/unconscious）会自动打断专注。

## 五、实时行为（dndclasses 已强制）
- `incapacitated`：禁攻击/交互/使用物品。
- `immobile`：客户端 mixin 锁移动与跳跃 + 服务端清零水平速度。
- `half_damage`：受到伤害减半（石化）。
- `cannot_attack_source`：不能攻击来源实体（魅惑）。
- `cannot_approach_source`：服务端钳制，不能靠近来源（恐惧）。
- `blind/invisible/weakness`：对应 MC 药水效果（由 JSON `status_effects` 配）。
- 力竭：L2 速度减半、L4 生命上限减半、L5 速度 0、L6 死亡；长休 -1。
- 死亡/重生清空所有条件（力竭保留）。

## 六、HUD（预留）
暂不画屏幕 HUD，只有动作栏提示。未来 HUD 直接用：
- 客户端：`mdh.dndclasses.client.ClientConditionState.getActive()`（已随包同步，含每条 `flags/duration/source`）。
- 服务端：`ConditionManager.snapshot(player)`。

## 七、武器（合并后对 spell 的影响）
- 物品 id：`dndweapon:*` → **`dndclasses:*`**（材质 `iron/adamantine/silver` × 37 种 + 弹药）。
- 伤害类型：`dndclasses:acid/bludgeoning/.../thunder`（13 种）。
- 若 dndspells 里引用过 `dndweapon:` 的资源/配方/代码，需要全部改成 `dndclasses:`。

## 八、构建
`gradlew build` 后把 `build/libs/dndclasses-1.0.0.jar` 覆盖 `E:\dndspells\libs\dndclasses-1.0.0.jar`（已由 dndclasses 侧完成），然后在 dndspells 做一次 Gradle sync。
