package mdh.dndclasses.data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RaceData {

    public record Subrace(String key, String displayName, Map<String, Integer> asiBoosts, String description) {}
    public record RaceEntry(String displayName, List<Subrace> subraces, int speed, String size, int baseHp, String description) {}

    public static final Map<String, RaceEntry> RACES = new LinkedHashMap<>();

    static {
        RACES.put("dwarf", new RaceEntry("矮人", List.of(
            new Subrace("hill_dwarf", "丘陵矮人", Map.of("con", 2, "wis", 1), "+1感知, 每级+1HP"),
            new Subrace("mountain_dwarf", "山脉矮人", Map.of("con", 2, "str", 2), "+2力量, 轻甲/中甲熟练")
        ), 25, "中型", 0, "坚韧的古老种族，以力量和传统为荣。"));

        RACES.put("elf", new RaceEntry("精灵", List.of(
            new Subrace("high_elf", "高等精灵", Map.of("dex", 2, "int", 1), "+1智力, 戏法:法师之手"),
            new Subrace("wood_elf", "木精灵", Map.of("dex", 2, "wis", 1), "+1感知, 35尺速度, 隐藏"),
            new Subrace("dark_elf", "黑暗精灵", Map.of("dex", 2, "cha", 1), "+1魅力, 120尺黑暗视觉, 精灵魔法")
        ), 30, "中型", 0, "优雅而敏捷的奇幻生物。"));

        RACES.put("halfling", new RaceEntry("半身人", List.of(
            new Subrace("lightfoot", "轻足半身人", Map.of("dex", 2, "cha", 1), "+1魅力, 天生隐匿"),
            new Subrace("stout", "壮胆半身人", Map.of("dex", 2, "con", 1), "+1体质, 毒素豁免优势")
        ), 25, "小型", 0, "小巧而勇敢的冒险者。"));

        RACES.put("human", new RaceEntry("人类", List.of(
            new Subrace("standard_human", "标准人类", Map.of("str", 1, "dex", 1, "con", 1, "int", 1, "wis", 1, "cha", 1), "全属性+1"),
            new Subrace("variant_human", "变体人类", Map.of(), "自选两项不同属性各+1, 一项自选技能熟练, 一项自选专长")
        ), 30, "中型", 0, "最具适应性的人类，遍布大陆。"));

        RACES.put("dragonborn", new RaceEntry("龙裔", List.of(
            new Subrace("dragonborn", "龙裔", Map.of("str", 2, "cha", 1), "喷吐武器, 伤害抗性"),
            new Subrace("black", "黑龙(强酸)", Map.of("str", 2, "cha", 1), "喷吐武器:强酸, 强酸抗性"),
            new Subrace("blue", "蓝龙(闪电)", Map.of("str", 2, "cha", 1), "喷吐武器:闪电, 闪电抗性"),
            new Subrace("brass", "黄铜龙(火焰)", Map.of("str", 2, "cha", 1), "喷吐武器:火焰, 火焰抗性"),
            new Subrace("bronze", "青铜龙(闪电)", Map.of("str", 2, "cha", 1), "喷吐武器:闪电, 闪电抗性"),
            new Subrace("copper", "赤铜龙(强酸)", Map.of("str", 2, "cha", 1), "喷吐武器:强酸, 强酸抗性"),
            new Subrace("gold", "金龙(火焰)", Map.of("str", 2, "cha", 1), "喷吐武器:火焰, 火焰抗性"),
            new Subrace("green", "绿龙(毒素)", Map.of("str", 2, "cha", 1), "喷吐武器:毒素, 毒素抗性"),
            new Subrace("red", "红龙(火焰)", Map.of("str", 2, "cha", 1), "喷吐武器:火焰, 火焰抗性"),
            new Subrace("silver", "银龙(冷冻)", Map.of("str", 2, "cha", 1), "喷吐武器:冷冻, 冷冻抗性"),
            new Subrace("white", "白龙(冷冻)", Map.of("str", 2, "cha", 1), "喷吐武器:冷冻, 冷冻抗性")
        ), 30, "中型", 0, "龙血后裔，拥有龙族的力量。"));

        RACES.put("gnome", new RaceEntry("侏儒", List.of(
            new Subrace("forest_gnome", "森林侏儒", Map.of("int", 2, "dex", 1), "+1敏捷, 小幻术戏法"),
            new Subrace("rock_gnome", "岩石侏儒", Map.of("int", 2, "con", 1), "+1体质, 工匠知识")
        ), 25, "小型", 0, "聪明而幽默的小巧种族。"));

        RACES.put("half_elf", new RaceEntry("半精灵", List.of(
            new Subrace("half_elf", "半精灵", Map.of("cha", 2), "自选两项属性+1, 精灵血统")
        ), 30, "中型", 0, "人类与精灵的后裔。"));

        RACES.put("half_orc", new RaceEntry("半兽人", List.of(
            new Subrace("half_orc", "半兽人", Map.of("str", 2, "con", 1), "暴怒: 血量归零时保留1HP")
        ), 30, "中型", 0, "拥有兽人蛮力的混血种族。"));

        RACES.put("tiefling", new RaceEntry("提夫林", List.of(
            new Subrace("tiefling", "提夫林", Map.of("cha", 2, "int", 1), "黑暗视觉, 火焰抗性")
        ), 30, "中型", 0, "来自契约与诅咒的魔裔种族。"));
    }

    public static String getDisplayName(String raceKey) {
        RaceEntry entry = RACES.get(raceKey);
        return entry != null ? entry.displayName() : raceKey;
    }

    public static String getSubraceDisplayName(String raceKey, String subraceKey) {
        RaceEntry entry = RACES.get(raceKey);
        if (entry == null) return subraceKey;
        for (Subrace sr : entry.subraces()) {
            if (sr.key().equals(subraceKey)) return sr.displayName();
        }
        return subraceKey;
    }

    public static Subrace getSubrace(String raceKey, String subraceKey) {
        RaceEntry entry = RACES.get(raceKey);
        if (entry == null) return null;
        for (Subrace sr : entry.subraces()) {
            if (sr.key().equals(subraceKey)) return sr;
        }
        return null;
    }

    public static boolean isValidRace(String key) {
        return RACES.containsKey(key);
    }

    public static boolean isValidSubrace(String raceKey, String subraceKey) {
        RaceEntry entry = RACES.get(raceKey);
        if (entry == null) return false;
        for (Subrace sr : entry.subraces()) {
            if (sr.key().equals(subraceKey)) return true;
        }
        return false;
    }
}
