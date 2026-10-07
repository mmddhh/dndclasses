package mdh.dndclasses.data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClassRegistry {

    public record ClassEntry(String displayName, String hitDie, String primaryAbility, List<String> saveProficiencies, List<String> armorProficiencies, List<String> weaponProficiencies, String description) {}

    public static final Map<String, ClassEntry> CLASSES = new LinkedHashMap<>();

    static {
        CLASSES.put("barbarian", new ClassEntry("野蛮人", "d12", "力量", List.of("力量", "体质"), List.of("轻甲", "中甲", "盾牌"), List.of("军用武器", "简易武器"), "狂暴的战士，用愤怒摧毁敌人。"));
        CLASSES.put("bard", new ClassEntry("吟游诗人", "d8", "魅力", List.of("敏捷", "魅力"), List.of("轻甲"), List.of("手弩", "长剑", "细剑", "短剑", "简易武器"), "用音乐和魔法激励同伴的艺术家。"));
        CLASSES.put("cleric", new ClassEntry("牧师", "d8", "感知", List.of("感知", "魅力"), List.of("轻甲", "中甲", "盾牌"), List.of("简易武器"), "侍奉神祇的施法者。"));
        CLASSES.put("druid", new ClassEntry("德鲁伊", "d8", "感知", List.of("智力", "感知"), List.of("轻甲", "中甲", "盾牌—非金属"), List.of("俱乐部", "匕首", "标枪", "钉头锤", "硬头锤", "长棍", "弯刀", "镰刀", "投石索", "长矛"), "守护自然的古老施法者。"));
        CLASSES.put("fighter", new ClassEntry("战士", "d10", "力量或敏捷", List.of("力量", "体质"), List.of("所有护甲", "盾牌"), List.of("军用武器", "简易武器"), "武器大师，战场上的勇士。"));
        CLASSES.put("monk", new ClassEntry("武僧", "d8", "敏捷与感知", List.of("力量", "敏捷"), List.of("无"), List.of("简易武器", "短剑"), "以气为力量的武术大师。"));
        CLASSES.put("paladin", new ClassEntry("圣骑士", "d10", "力量与魅力", List.of("感知", "魅力"), List.of("所有护甲", "盾牌"), List.of("军用武器", "简易武器"), "誓约守护者，圣光的使者。"));
        CLASSES.put("ranger", new ClassEntry("游侠", "d10", "敏捷与感知", List.of("力量", "敏捷"), List.of("轻甲", "中甲", "盾牌"), List.of("军用武器", "简易武器"), "荒野中的追踪者和猎手。"));
        CLASSES.put("rogue", new ClassEntry("游荡者", "d8", "敏捷", List.of("敏捷", "智力"), List.of("轻甲"), List.of("手弩", "长剑", "细剑", "短剑", "简易武器"), "阴影中的潜行者和偷袭专家。"));
        CLASSES.put("sorcerer", new ClassEntry("术士", "d6", "魅力", List.of("体质", "魅力"), List.of("无"), List.of("匕首", "投石索", "长棍", "轻弩"), "体内流淌着魔法之血的施法者。"));
        CLASSES.put("warlock", new ClassEntry("邪术师", "d8", "魅力", List.of("感知", "魅力"), List.of("轻甲"), List.of("简易武器"), "与强大存在缔结契约的施法者。"));
        CLASSES.put("wizard", new ClassEntry("法师", "d6", "智力", List.of("智力", "感知"), List.of("无"), List.of("匕首", "投石索", "长棍", "轻弩"), "通过研习掌握奥术的学者。"));
    }

    public static String getDisplayName(String key) {
        ClassEntry entry = CLASSES.get(key);
        return entry != null ? entry.displayName() : key;
    }

    public static boolean isValid(String key) {
        return CLASSES.containsKey(key);
    }
}
