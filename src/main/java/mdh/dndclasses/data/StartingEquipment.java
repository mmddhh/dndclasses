package mdh.dndclasses.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StartingEquipment {

    public record EquipmentItem(String id, int count) {}
    public record EquipmentChoice(String label, List<EquipmentItem> items) {}
    public record ClassEquipment(List<EquipmentChoice> choices) {}

    public static final Map<String, ClassEquipment> EQUIPMENT = new LinkedHashMap<>();

    private static EquipmentItem item(String id) {
        return new EquipmentItem(id, 1);
    }

    private static EquipmentItem stack(String id, int count) {
        return new EquipmentItem(id, count);
    }

    private static EquipmentItem[] armorSet(String prefix) {
        return new EquipmentItem[]{
                item(prefix + "_helmet"),
                item(prefix + "_chestplate"),
                item(prefix + "_leggings"),
                item(prefix + "_boots")
        };
    }

    private static List<EquipmentItem> kit(EquipmentItem... items) {
        return List.of(items);
    }

    private static List<EquipmentItem> withArmor(String prefix, EquipmentItem... extras) {
        List<EquipmentItem> items = new ArrayList<>(List.of(armorSet(prefix)));
        items.addAll(List.of(extras));
        return List.copyOf(items);
    }

    static {
        EQUIPMENT.put("barbarian", new ClassEquipment(List.of(
                new EquipmentChoice("选项A：巨斧 + 手斧×2",
                        kit(item("dndclasses:iron_greataxe"), item("dndclasses:iron_handaxe"), item("dndclasses:iron_handaxe"))),
                new EquipmentChoice("选项B：长剑",
                        kit(item("dndclasses:iron_longsword")))
        )));

        EQUIPMENT.put("bard", new ClassEquipment(List.of(
                new EquipmentChoice("选项A：细剑 + 匕首 + 皮甲全套",
                        withArmor("dndclasses:iron_leather", item("dndclasses:iron_rapier"), item("dndclasses:iron_dagger"))),
                new EquipmentChoice("选项B：长剑 + 匕首 + 皮甲全套",
                        withArmor("dndclasses:iron_leather", item("dndclasses:iron_longsword"), item("dndclasses:iron_dagger")))
        )));

        EQUIPMENT.put("cleric", new ClassEquipment(List.of(
                new EquipmentChoice("选项A：钉头锤 + 盾牌 + 鳞甲全套",
                        withArmor("dndclasses:iron_scale_mail", item("dndclasses:iron_mace"), item("minecraft:shield"))),
                new EquipmentChoice("选项B：战锤 + 盾牌 + 鳞甲全套",
                        withArmor("dndclasses:iron_scale_mail", item("dndclasses:iron_warhammer"), item("minecraft:shield")))
        )));

        EQUIPMENT.put("druid", new ClassEquipment(List.of(
                new EquipmentChoice("选项A：长棍 + 皮甲全套",
                        withArmor("dndclasses:iron_leather", item("dndclasses:iron_quarterstaff"))),
                new EquipmentChoice("选项B：弯刀 + 盾牌 + 皮甲全套",
                        withArmor("dndclasses:iron_leather", item("dndclasses:iron_scimitar"), item("minecraft:shield")))
        )));

        EQUIPMENT.put("fighter", new ClassEquipment(List.of(
                new EquipmentChoice("选项A：链甲全套 + 盾牌 + 长剑",
                        withArmor("dndclasses:iron_chain_mail", item("minecraft:shield"), item("dndclasses:iron_longsword"))),
                new EquipmentChoice("选项B：链甲全套 + 巨剑",
                        withArmor("dndclasses:iron_chain_mail", item("dndclasses:iron_greatsword")))
        )));

        EQUIPMENT.put("monk", new ClassEquipment(List.of(
                new EquipmentChoice("选项A：短剑 + 匕首",
                        kit(item("dndclasses:iron_shortsword"), item("dndclasses:iron_dagger"))),
                new EquipmentChoice("选项B：长棍",
                        kit(item("dndclasses:iron_quarterstaff")))
        )));

        EQUIPMENT.put("paladin", new ClassEquipment(List.of(
                new EquipmentChoice("选项A：链甲全套 + 盾牌 + 长剑",
                        withArmor("dndclasses:iron_chain_mail", item("minecraft:shield"), item("dndclasses:iron_longsword"))),
                new EquipmentChoice("选项B：板甲全套 + 巨剑",
                        withArmor("dndclasses:iron_plate", item("dndclasses:iron_greatsword")))
        )));

        EQUIPMENT.put("ranger", new ClassEquipment(List.of(
                new EquipmentChoice("选项A：皮甲全套 + 短剑 + 长弓 + 箭×20",
                        withArmor("dndclasses:iron_leather", item("dndclasses:iron_shortsword"),
                                item("dndclasses:iron_longbow"), stack("dndclasses:iron_arrow", 20))),
                new EquipmentChoice("选项B：皮甲全套 + 双短剑",
                        withArmor("dndclasses:iron_leather", item("dndclasses:iron_shortsword"), item("dndclasses:iron_shortsword")))
        )));

        EQUIPMENT.put("rogue", new ClassEquipment(List.of(
                new EquipmentChoice("选项A：皮甲全套 + 细剑 + 短弓 + 箭×20",
                        withArmor("dndclasses:iron_leather", item("dndclasses:iron_rapier"),
                                item("dndclasses:iron_shortbow"), stack("dndclasses:iron_arrow", 20))),
                new EquipmentChoice("选项B：皮甲全套 + 双短剑",
                        withArmor("dndclasses:iron_leather", item("dndclasses:iron_shortsword"), item("dndclasses:iron_shortsword")))
        )));

        EQUIPMENT.put("sorcerer", new ClassEquipment(List.of(
                new EquipmentChoice("选项A：轻弩 + 匕首 + 弩矢×20",
                        kit(item("dndclasses:iron_light_crossbow"), item("dndclasses:iron_dagger"),
                                stack("dndclasses:iron_bolt", 20))),
                new EquipmentChoice("选项B：长棍 + 匕首",
                        kit(item("dndclasses:iron_quarterstaff"), item("dndclasses:iron_dagger")))
        )));

        EQUIPMENT.put("warlock", new ClassEquipment(List.of(
                new EquipmentChoice("选项A：轻弩 + 匕首 + 弩矢×20",
                        kit(item("dndclasses:iron_light_crossbow"), item("dndclasses:iron_dagger"),
                                stack("dndclasses:iron_bolt", 20))),
                new EquipmentChoice("选项B：长棍 + 匕首",
                        kit(item("dndclasses:iron_quarterstaff"), item("dndclasses:iron_dagger")))
        )));

        EQUIPMENT.put("wizard", new ClassEquipment(List.of(
                new EquipmentChoice("选项A：长棍 + 匕首",
                        kit(item("dndclasses:iron_quarterstaff"), item("dndclasses:iron_dagger"))),
                new EquipmentChoice("选项B：轻弩 + 匕首 + 弩矢×20",
                        kit(item("dndclasses:iron_light_crossbow"), item("dndclasses:iron_dagger"),
                                stack("dndclasses:iron_bolt", 20)))
        )));
    }

    public static ClassEquipment get(String classKey) {
        return EQUIPMENT.get(classKey);
    }

    public static List<EquipmentItem> getItems(String classKey, int choiceIndex) {
        ClassEquipment eq = EQUIPMENT.get(classKey);
        if (eq == null || choiceIndex < 0 || choiceIndex >= eq.choices().size()) {
            return List.of();
        }
        return eq.choices().get(choiceIndex).items();
    }
}
