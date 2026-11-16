# M1 验收标准（规划 → 世界）

## 范围

M1 只验证“计划能可靠地变成游戏内内容”，不验证选址质量、建筑美观或完整聚落。

## 自动检查

```
/worldplan selftest
```

通过标准：输出 `M1 selftest: 5/5 通过`。

检查项：

1. 计划规模：6 国 / 30 聚落 / 30 事件；
2. `/worldplan verify` 无错误；
3. 区块缓存与计划一致（每个聚落都能按 chunk 找到）；
4. `dndclasses:planned_ruin` Feature 已注册；
5. configured/placed feature 数据文件存在。

## 手动检查

### 计划持久化

```
/worldplan
```

退出重进世界后再执行一次，国家/聚落/事件应完全一致。

### 指定建造

```
/worldplan info <id>
/worldplan tp <id>
/worldplan build <id>
```

通过标准：

- 传送落在可站立地表，不在水里/基岩；
- 建筑出现在聚落坐标附近；
- 材料按当地地形选择；
- 屋顶、窗、地面纹样随聚落文化变化。

### 自动放置

必须使用**新世界**或**从未生成过的区块**。

1. 创建新世界；
2. `/worldplan tp <id>`；
3. 观察目标区块是否出现测试塔。

通过标准：

- 区块生成时自动出现塔；
- 不崩溃、不卡主线程；
- 传送命令显式补建不会产生重复堆叠。

### NPC

```
/npc list
/npc spawn <id>
```

通过标准：

- 村民生成；
- `/data get entity @e[type=minecraft:villager,limit=1,sort=nearest] ForgeData`
  能看到 `DndNation` / `DndSettlement` / `DndCulture` / `DndRole`。

## 本阶段不验收

- 聚落是否在海里/悬崖上（选址算法属于 M2）；
- 建筑是否美观、是否完整聚落；
- 道路、遗迹、奇观；
- NPC 对话、交易、AI；
- 跨版本/跨加载器；
- 文化扩散与贸易影响。

## 已知限制

- 聚落坐标是确定性网格 + 抖动，不是地形评分；
- 海洋聚落会在 `/worldplan tp` 时自动找陆地并改坐标；
- 已生成的旧区块不会回溯补塔；
- 测试塔只有 9×9，不是最终聚落建筑。
