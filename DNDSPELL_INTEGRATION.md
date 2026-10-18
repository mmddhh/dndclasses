# dndspell 集成说明(由 dndspells 侧提交,请过目)

> 背景:dndspells 要接"长休准备法术 / 升级调法术 / 法师法术书"。为了能编译,我在 dndClasses 里做了**一处最小改动**(方案里的 "A. dndclasses 侧")。本文件说明改了什么、为什么、以及 dndspells 对 dndClasses 的依赖约定。若与你们内部实现冲突,以你们为准,改动可以合并/重做。

## 一、本次对 dndClasses 的改动(3 个文件)

### 1. `events/LevelUp.java`(重写)
```java
// 旧:LevelUp(int newlevel) + getnewlevel()
// 新:
public class LevelUp extends Event {
    private final ServerPlayer player;
    private final int oldLevel;
    private final int newLevel;
    public LevelUp(ServerPlayer player, int oldLevel, int newLevel){ ... }
    public ServerPlayer getPlayer(){ return player; }
    public int getOldLevel(){ return oldLevel; }
    public int getNewLevel(){ return newLevel; }
}
```
原因:原来 `LevelUp` 不带玩家,dndspells 无法在升级时弹出"选择新法术"界面。

### 2. `capability/ModForgeEvents.java`(`onLivingDeath`,1 行)
```java
// 旧
MinecraftForge.EVENT_BUS.post(new LevelUp(newLevel));
// 新
MinecraftForge.EVENT_BUS.post(new LevelUp(player, oldLevel, newLevel));
```

### 3. `command/DndCommands.java`(2 个 import + 3 处)
- 新增 import:`mdh.dndclasses.events.LevelUp`、`net.minecraftforge.common.MinecraftForge`。
- `xpAdd` / `xpSet` / `levelSet`:记录 `int before = level.getlevel();`,等级上升时 post:
```java
MinecraftForge.EVENT_BUS.post(new LevelUp(player, before, level.getlevel()));
```
原因:这三个命令改级时**原本没有 post 任何事件**,dndspells 收不到升级。

> 编译:`gradlew -p E:\dndClasses jar --offline`(已 BUILD SUCCESSFUL)。产物 `build/libs/dndclasses-1.0.0.jar`,已拷入 `E:\dndspells\libs\`。

## 二、dndspells 依赖的 dndClasses 约定(现状,已满足)
dndspells **不再**需要 dndClasses 做下面这些(自行实现):
- 法师法术书 = dndspells 的物品(`wizard_spellbook`,内容在该物品 NBT);dndClasses 只存 `preparedSpells` 准备列表(已有)。
- 休息后客户端刷新:dndspells 在 `DndLongRestEvent`/`DndShortRestEvent` 里直接调公开的 `ServerStateSync.send(player)`(所以"休息后轮盘次数不刷新"的问题不用你们改)。

dndspells 会调用的公开 API(请勿随意改签名):
- `capability.ModCapabilities.{DND_SPELL,LEVEL,ABILITY}_CAPABILITY`
- `server.spells.IDndSpell`(`get/setPreparedSpells`、`getKnownCantrips/Spells`、`getPreparedSpellLimit`、`getKnownCantrip/SpellLimit`、`getmaxspellslots`、`Preparation`)
- `network.ServerStateSync.send(ServerPlayer)`
- `events.{DndCharacterCreatedEvent, DndLongRestEvent, DndShortRestEvent, LevelUp}`
- `level.Ilevel`、`ability.Iability`、`data.ClassRegistry`

## 三、后续可能还会需要的(未来,非本期)
1. **结构化豁免熟练**:`ClassRegistry.ClassEntry.saveProficiencies` 现在是中文显示名("力量"/"体质")。施法效果层需要机器键,建议加 `saves: ["str","con"]` 或提供 `ClassRegistry.getSaveProficiencies(classKey)`。
2. **专注(concentration)**:存"当前专注法术 + 剩余 tick"、受伤打断。dndspells 也可自建 capability,但若 dndClasses 提供会更统一。
3. **HP(职业生命骰 + 体质调整值)**:若做在 dndClasses,建议顺带暴露 `getMaxHp(player)` 给 HUD。

## 四、如何复现/重做
如需你们自己重做本次改动:按上面三点改 `LevelUp.java`、`ModForgeEvents.onLivingDeath`、`DndCommands` 的 `xpAdd/xpSet/levelSet`,再 `gradlew -p E:\dndClasses jar --offline` 并把 `build/libs/dndclasses-1.0.0.jar` 覆盖到 `E:\dndspells\libs\`。

## 五、更新记录(第 2 次改动)
### `capability/ModForgeEvents.java` — `onPlayerWakeUp` 的长休判定(1 行)
```java
// 旧
if (!event.wakeImmediately() && event.updateLevel()) {
// 新
if (!event.wakeImmediately()) {
```
原因:`PlayerWakeUpEvent(wakeImmediately, updateLevel)` 的布尔来自 `Player.stopSleepInBed(...)`,实测各路径:
- **夜里睡够 → 跳夜自然醒**(`ServerLevel.wakeUpAllPlayers`):`stopSleepInBed(false, false)` → 长休应触发
- 早晨到点自然醒(`Player.tick`):`(false, true)` → 也应触发
- 主动起床/命令:`(true, true)` → 不该触发
- 受伤:`(true, ...)`;断线:`(true, false)`

即 **`wakeImmediately` 才是"自然醒 vs 立刻起身"**,而 `updateLevel` 的语义是"是否由客户端发起、需要通知服务器"(跳夜时反而是 false)。所以正确判定是 **`!event.wakeImmediately()`**:自然醒触发、主动起床/受伤/断线不触发。

> 修改后重编:`gradlew -p E:\dndClasses jar --offline`,`build/libs/dndclasses-1.0.0.jar` 覆盖到 `E:\dndspells\libs\`。

## 六、第 3 次(dndclasses 侧已完成)
已落地并覆盖 jar:
- `ClassRegistry.saveProficiencies` → 机器键(str/dex/…)+ `getSaveProficiencies(key)`(未知空表)+ `getHitDieValue(key)`。
- `IDndSpell.getMaxSpellLevel()` default 方法。
- `HealthManager.getMaxHp(ServerPlayer): double` + `apply()`(由 `CharacterRefresh.refresh` 调用,写 `Attributes.MAX_HEALTH` base)。
- `DndClassChangedEvent(ServerPlayer, oldKey, newKey)`(无触发点)。
- `ModForgeEvents` 在长休/短休处补了 `ServerStateSync.send`。

dndspells 侧已跟进:改用 `cap.getMaxSpellLevel()`;去掉重复的休息同步(`onShortRest` 删除、`onLongRest` 不再 send)。`onLevelUp` 仍保留一次 `ServerStateSync.send`,因为 dndclasses 在 XP 路径上是"先 post LevelUp、后 sync",dndspells 需先刷新上限再建页面。
