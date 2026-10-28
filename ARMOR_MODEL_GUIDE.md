# D&D 护甲 3D 模型接入指南（dndclasses 侧）

> 当前版本（本轮实现）**只做了占位资源**：144 件护甲的物品图标模型、6 张人形护甲 layer 贴图、144 条配方与语言键。真正的 3D 模型（每套/每件不同外形）留到下一轮，本文件说明如何在不改规则代码的情况下接入。

## 一、当前资源结构

| 资源 | 位置 | 说明 |
| --- | --- | --- |
| 物品图标 | `assets/dndclasses/models/item/<material>_<type>_<slot>.json` | 占位：铁→原版铁甲、精金→原版下界合金甲、秘银→原版金甲图标 |
| 人形护甲贴图 | `assets/dndclasses/textures/models/armor/<material>_layer_1.png` | 64×32，头盔/胸甲/手臂（第一层） |
| 人形护甲贴图 | `assets/dndclasses/textures/models/armor/<material>_layer_2.png` | 64×32，护腿/靴子（第二层） |
| 物品注册 | `armor/ArmorItems.java` | id = `<material>_<type>_<slot>`，共 3×12×4 = 144 |
| 材质 | `armor/DndArmorMaterial.java` / `ArmorMaterialType.java` | `getName()` = `dndclasses:<material>` |

`ArmorMaterial#getName()` 在 1.20.1 里是 `String`，原版 `HumanoidArmorLayer` 会按 `namespace:path` 拆分并拼成：

```
<namespace>:textures/models/armor/<path>_layer_<1|2>.png
```

因此 `dndclasses:iron` → `dndclasses:textures/models/armor/iron_layer_1.png` / `iron_layer_2.png`。换成自己的贴图时**不要改 `getName()`**，直接覆盖这两个文件即可按材质生效。

## 二、方案 A：原版 `HumanoidModel` + `IClientItemExtensions`（推荐，无额外依赖）

1. 新建模型类（示例，继承 `HumanoidModel<LivingEntity>`，在构造里挂自己的 `ModelPart`）：

```java
public class PlateArmorModel extends HumanoidModel<LivingEntity> {
    public PlateArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        // 在这里加/改 root 下的部件（复制原版 humanoid 结构后改尺寸/位置）
        return LayerDefinition.create(mesh, 64, 32);
    }
}
```

2. 注册模型层（`EntityRenderersEvent.RegisterLayerDefinitions`，client bus）：

```java
event.registerLayerDefinition(PLATE_LAYER, PlateArmorModel::createBodyLayer);
```

3. 给物品注册客户端扩展（`RegisterClientExtensionsEvent`，client bus）：

```java
event.registerItem(new IClientItemExtensions() {
    private PlateArmorModel model;

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                                                  EquipmentSlot slot, HumanoidModel<?> original) {
        if (model == null) {
            model = new PlateArmorModel(
                Minecraft.getInstance().getEntityModels().bakeLayer(PLATE_LAYER));
        }
        // 按 slot 复制可见性/姿态，避免手臂、腿穿插
        model.copyFrom(original);
        model.setAllVisible(true);
        return model;
    }
}, ArmorItems.ARMOR.get("iron_plate_chestplate").get() /* 以及其余 143 件 */);
```

> `IClientItemExtensions` 的注册是**逐物品**的。可以按“每套护甲一个模型”批量遍历
> `ArmorItems.ARMOR`，按 `DndArmorItem#armorType()` 选模型，再统一注册。
> 不要用 `getArmorModel` 旧接口——1.20.1 已改成 `IClientItemExtensions`。

4. 贴图仍走 `textures/models/armor/<material>_layer_1/2.png`；如果某一套要独立贴图，
   把 `DndArmorMaterial` 改成按 `ArmorType` 返回不同 name（例如 `dndclasses:plate`），
   并新增对应 `plate_layer_1/2.png`。注意 `DndArmorMaterial` 目前按**材质**共享，
   改之前要确认 `ArmorManager` 的整套判定不受影响（它只用 `ArmorType`/`ArmorMaterialType` 枚举）。

## 三、方案 B：GeckoLib `GeoArmorRenderer`（需要 GeckoLib 依赖）

适合“每件护甲一个 `.geo.json` + 动画”的做法：

1. 物品类实现 `GeoItem`（或在 `DndArmorItem` 上实现）：

```java
public class GeoDndArmorItem extends DndArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GeoDndArmorItem(ArmorType type, ArmorMaterialType material, ArmorItem.Type slot, Properties props) {
        super(type, material, slot, props);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<?> renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    @Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot,
                    @Nullable HumanoidModel<T> original) {
                if (renderer == null) {
                    renderer = new GeoArmorRenderer<>(new PlateArmorRenderer());
                }
                return renderer;
            }
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
```

2. `PlateArmorRenderer extends GeoArmorRenderer<GeoDndArmorItem>`，`getTextureResource` 指向
   `assets/dndclasses/textures/armor/...`，`getModelResource` 指向 `geo/...geo.json`。

3. 注意 GeckoLib 的 `GeoArmorRenderer` 会接管整套渲染；与方案 A 的 layer 贴图**二选一**，
   不要把两套同时挂到同一件物品上。

## 四、占位资源替换清单（只改资源、不改 Java）

| 想改什么 | 改哪里 |
| --- | --- |
| 物品栏图标 | `assets/dndclasses/models/item/<material>_<type>_<slot>.json` 的 `layer0`，或新增 `assets/dndclasses/textures/item/<id>.png` 后把 `layer0` 指过去 |
| 穿在身上的外观（同材质共用） | `assets/dndclasses/textures/models/armor/<material>_layer_1.png`、`_layer_2.png` |
| 单套独立外观（同材质不同套） | 改 `DndArmorMaterial` 的 name 生成规则 + 新增 `<name>_layer_1/2.png`；或走方案 A/B |
| 中文/英文名 | `assets/dndclasses/lang/zh_cn.json` / `en_us.json` 的 `item.dndclasses.<id>` |

## 五、验证清单

1. `gradlew compileJava` 通过（模型类只依赖 client 类，注意放在 `client` 包或加 `@OnlyIn(Dist.CLIENT)`/`Dist.CLIENT` 事件）。
2. 穿上同材质同类型的 4 件 → O 键角色页 AC 显示套装 AC；混搭 → 只有 `10+敏捷`。
3. 精金整套：怪物暴击不再 ×1.5；秘银整套：力量不足也不掉移速、翻滚距离不打折。
4. 护甲耐久到 0：物品不消失，提示损坏；铁砧 + 对应锭修复后恢复。
5. 原版护甲：配方/创造栏/战利品/村民交易均不再产出；`getArmorValue()` 对玩家恒为 0。
