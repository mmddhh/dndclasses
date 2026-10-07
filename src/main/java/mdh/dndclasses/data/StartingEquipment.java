package mdh.dndclasses.data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StartingEquipment {

    public record EquipmentChoice(String label, List<String> items) {}
    public record ClassEquipment(List<EquipmentChoice> choices) {}

    public static final Map<String, ClassEquipment> EQUIPMENT = new LinkedHashMap<>();

    static {
        EQUIPMENT.put("barbarian", new ClassEquipment(List.of(
            new EquipmentChoice("选项A: 巨斧 + 手斧×2", List.of("minecraft:iron_axe", "minecraft:iron_axe", "minecraft:iron_axe")),
            new EquipmentChoice("选项B: 军用近战武器", List.of("minecraft:iron_sword"))
        )));

        EQUIPMENT.put("bard", new ClassEquipment(List.of(
            new EquipmentChoice("选项A: 细剑 + 匕首", List.of("minecraft:iron_sword", "minecraft:iron_sword")),
            new EquipmentChoice("选项B: 长剑 + 匕首", List.of("minecraft:iron_sword", "minecraft:iron_sword"))
        )));

        EQUIPMENT.put("cleric", new ClassEquipment(List.of(
            new EquipmentChoice("选项A: 钉头锤 + 盾牌", List.of("minecraft:iron_axe", "minecraft:shield")),
            new EquipmentChoice("选项B: 硬头锤 + 盾牌", List.of("minecraft:wooden_axe", "minecraft:shield"))
        )));

        EQUIPMENT.put("druid", new ClassEquipment(List.of(
            new EquipmentChoice("选项A: 橡木法杖", List.of("minecraft:stick")),
            new EquipmentChoice("选项B: 弯刀 + 木盾", List.of("minecraft:iron_sword", "minecraft:shield"))
        )));

        EQUIPMENT.put("fighter", new ClassEquipment(List.of(
            new EquipmentChoice("选项A: 链甲 + 盾牌 + 长剑", List.of("minecraft:chainmail_chestplate", "minecraft:shield", "minecraft:iron_sword")),
            new EquipmentChoice("选项B: 链甲 + 巨剑", List.of("minecraft:chainmail_chestplate", "minecraft:iron_sword"))
        )));

        EQUIPMENT.put("monk", new ClassEquipment(List.of(
            new EquipmentChoice("选项A: 短剑 + 匕首", List.of("minecraft:iron_sword", "minecraft:iron_sword")),
            new EquipmentChoice("选项B: 长棍", List.of("minecraft:stick"))
        )));

        EQUIPMENT.put("paladin", new ClassEquipment(List.of(
            new EquipmentChoice("选项A: 链甲 + 盾牌 + 长剑", List.of("minecraft:chainmail_chestplate", "minecraft:shield", "minecraft:iron_sword")),
            new EquipmentChoice("选项B: 板甲 + 巨剑", List.of("minecraft:diamond_chestplate", "minecraft:iron_sword"))
        )));

        EQUIPMENT.put("ranger", new ClassEquipment(List.of(
            new EquipmentChoice("选项A: 皮甲 + 短剑 + 长弓", List.of("minecraft:leather_chestplate", "minecraft:iron_sword", "minecraft:bow")),
            new EquipmentChoice("选项B: 皮甲 + 双短剑", List.of("minecraft:leather_chestplate", "minecraft:iron_sword", "minecraft:iron_sword"))
        )));

        EQUIPMENT.put("rogue", new ClassEquipment(List.of(
            new EquipmentChoice("选项A: 细剑 + 短弓", List.of("minecraft:iron_sword", "minecraft:bow")),
            new EquipmentChoice("选项B: 双短剑", List.of("minecraft:iron_sword", "minecraft:iron_sword"))
        )));

        EQUIPMENT.put("sorcerer", new ClassEquipment(List.of(
            new EquipmentChoice("选项A: 轻弩 + 匕首", List.of("minecraft:crossbow", "minecraft:iron_sword")),
            new EquipmentChoice("选项B: 长棍 + 匕首", List.of("minecraft:stick", "minecraft:iron_sword"))
        )));

        EQUIPMENT.put("warlock", new ClassEquipment(List.of(
            new EquipmentChoice("选项A: 轻弩 + 匕首", List.of("minecraft:crossbow", "minecraft:iron_sword")),
            new EquipmentChoice("选项B: 长棍 + 匕首", List.of("minecraft:stick", "minecraft:iron_sword"))
        )));

        EQUIPMENT.put("wizard", new ClassEquipment(List.of(
            new EquipmentChoice("选项A: 长棍 + 匕首", List.of("minecraft:stick", "minecraft:iron_sword")),
            new EquipmentChoice("选项B: 轻弩 + 匕首", List.of("minecraft:crossbow", "minecraft:iron_sword"))
        )));
    }

    public static ClassEquipment get(String classKey) {
        return EQUIPMENT.get(classKey);
    }

    public static List<String> getItems(String classKey, int choiceIndex) {
        ClassEquipment eq = EQUIPMENT.get(classKey);
        if (eq == null || choiceIndex < 0 || choiceIndex >= eq.choices().size()) return List.of();
        return eq.choices().get(choiceIndex).items();
    }
}
