package mdh.dndclasses.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PHB race / subrace data: ability bonuses, speed, traits and the flexible-ASI count.
 * <p>
 * Traits are ids only ({@code darkvision_60}, {@code lucky}, ...); the actual mechanics are
 * implemented by the consumer (this mod now only applies ASI + speed, the rest is left to turn).
 */
public final class RaceData {

    public static final List<String> ABILITIES = List.of("str", "dex", "con", "int", "wis", "cha");

    public record Subrace(String key, String displayName, Map<String, Integer> asiBoosts,
                          int asiChoiceCount, int speed, List<String> traits, String description) {
    }

    public record RaceEntry(String displayName, List<Subrace> subraces, int speed, String size,
                            int baseHp, String description) {
    }

    public static final Map<String, RaceEntry> RACES = new LinkedHashMap<>();

    static {
        RACES.put("dwarf", new RaceEntry("矮人", List.of(
                new Subrace("hill_dwarf", "丘陵矮人", Map.of("con", 2, "wis", 1), 0, 0,
                        List.of("darkvision_60", "dwarf_resilience", "stonecunning",
                                "dwarf_combat_training", "tool_proficiency"),
                        "+1感知，每级+1生命值"),
                new Subrace("mountain_dwarf", "山脉矮人", Map.of("con", 2, "str", 2), 0, 0,
                        List.of("darkvision_60", "dwarf_resilience", "stonecunning",
                                "dwarf_combat_training", "dwarf_armor_training"),
                        "+2力量，轻甲/中甲熟练")
        ), 25, "medium", 0, "坚韧而顽强的古老种族，重视荣誉与传统。"));

        RACES.put("elf", new RaceEntry("精灵", List.of(
                new Subrace("high_elf", "高等精灵", Map.of("dex", 2, "int", 1), 0, 0,
                        List.of("darkvision_60", "keen_senses", "fey_ancestry", "trance",
                                "elf_weapon_training", "high_elf_cantrip"),
                        "+1智力，一个法师戏法"),
                new Subrace("wood_elf", "木精灵", Map.of("dex", 2, "wis", 1), 0, 35,
                        List.of("darkvision_60", "keen_senses", "fey_ancestry", "trance",
                                "elf_weapon_training", "fleet_of_foot", "mask_of_the_wild"),
                        "+1感知，速度 35 尺，可尝试隐藏"),
                new Subrace("dark_elf", "黑暗精灵", Map.of("dex", 2, "cha", 1), 0, 0,
                        List.of("superior_darkvision_120", "sunlight_sensitivity", "drow_magic",
                                "keen_senses", "fey_ancestry", "trance"),
                        "+1魅力，120 尺黑暗视觉，阳光敏感")
        ), 30, "medium", 0, "优雅而长寿的种族，与自然和魔法亲近。"));

        RACES.put("halfling", new RaceEntry("半身人", List.of(
                new Subrace("lightfoot", "轻足半身人", Map.of("dex", 2, "cha", 1), 0, 0,
                        List.of("lucky", "brave", "halfling_nimbleness"),
                        "+1魅力，天生善于隐匿"),
                new Subrace("stout", "壮胆半身人", Map.of("dex", 2, "con", 1), 0, 0,
                        List.of("lucky", "brave", "halfling_nimbleness", "stout_resilience"),
                        "+1体质，毒素豁免优势")
        ), 25, "small", 0, "身材矮小却勇敢幸运的冒险者。"));

        RACES.put("human", new RaceEntry("人类", List.of(
                new Subrace("standard_human", "标准人类",
                        Map.of("str", 1, "dex", 1, "con", 1, "int", 1, "wis", 1, "cha", 1), 0, 0,
                        List.of(), "全属性+1"),
                new Subrace("variant_human", "变体人类", Map.of(), 2, 0,
                        List.of("skill_versatility", "feat"),
                        "自选两项不同属性各+1，一项技能熟练，一项专长")
        ), 30, "medium", 0, "最具适应力和野心的人类。"));

        List<Subrace> dragonborn = new ArrayList<>();
        dragonborn.add(new Subrace("dragonborn", "龙裔", Map.of("str", 2, "cha", 1), 0, 0,
                List.of("draconic_ancestry", "breath_weapon", "damage_resistance"),
                "喷吐武器，伤害抗性"));
        for (String color : List.of("black", "blue", "brass", "bronze", "copper",
                "gold", "green", "red", "silver", "white")) {
            dragonborn.add(new Subrace(color, color + " 龙裔", Map.of("str", 2, "cha", 1), 0, 0,
                    List.of("draconic_ancestry", "breath_weapon", "damage_resistance",
                            "draconic_" + color),
                    "龙裔先祖：" + color));
        }
        RACES.put("dragonborn", new RaceEntry("龙裔", List.copyOf(dragonborn), 30, "medium", 0,
                "拥有龙族血脉，掌握喷吐武器与伤害抗性。"));

        RACES.put("gnome", new RaceEntry("侏儒", List.of(
                new Subrace("forest_gnome", "森林侏儒", Map.of("int", 2, "dex", 1), 0, 0,
                        List.of("darkvision_60", "gnome_cunning", "natural_illusionist"),
                        "+1敏捷，小型幻术戏法"),
                new Subrace("rock_gnome", "岩石侏儒", Map.of("int", 2, "con", 1), 0, 0,
                        List.of("darkvision_60", "gnome_cunning", "artificers_lore", "tinker"),
                        "+1体质，工匠知识")
        ), 25, "small", 0, "聪明而幽默的小巧种族，天生的发明家。"));

        RACES.put("half_elf", new RaceEntry("半精灵", List.of(
                new Subrace("half_elf", "半精灵", Map.of("cha", 2), 2, 0,
                        List.of("darkvision_60", "fey_ancestry", "skill_versatility"),
                        "+2魅力，自选两项不同属性各+1")
        ), 30, "medium", 0, "人类与精灵的后裔，兼具适应力与魅力。"));

        RACES.put("half_orc", new RaceEntry("半兽人", List.of(
                new Subrace("half_orc", "半兽人", Map.of("str", 2, "con", 1), 0, 0,
                        List.of("darkvision_60", "menacing", "relentless_endurance", "savage_attacks"),
                        "+2力量 +1体质，坚韧而凶猛")
        ), 30, "medium", 0, "拥有兽人蛮力的混血种族。"));

        RACES.put("tiefling", new RaceEntry("提夫林", List.of(
                new Subrace("tiefling", "提夫林", Map.of("cha", 2, "int", 1), 0, 0,
                        List.of("darkvision_60", "hellish_resistance", "infernal_legacy"),
                        "+2魅力 +1智力，火焰抗性与炼狱魔法")
        ), 30, "medium", 0, "来自契约与诅咒的魔裔种族。"));
    }

    private RaceData() {
    }

    public static RaceEntry getRace(String key) {
        return RACES.get(key);
    }

    public static Subrace getSubrace(String raceKey, String subraceKey) {
        RaceEntry entry = RACES.get(raceKey);
        if (entry == null) {
            return null;
        }
        for (Subrace subrace : entry.subraces()) {
            if (subrace.key().equals(subraceKey)) {
                return subrace;
            }
        }
        return null;
    }

    public static boolean isValidRace(String key) {
        return RACES.containsKey(key);
    }

    public static boolean isValidSubrace(String raceKey, String subraceKey) {
        return getSubrace(raceKey, subraceKey) != null;
    }

    public static int speedFeet(String raceKey, String subraceKey) {
        RaceEntry entry = RACES.get(raceKey);
        Subrace subrace = getSubrace(raceKey, subraceKey);
        if (subrace != null && subrace.speed() > 0) {
            return subrace.speed();
        }
        return entry != null ? entry.speed() : 30;
    }

    public static int asiChoiceCount(String raceKey, String subraceKey) {
        Subrace subrace = getSubrace(raceKey, subraceKey);
        return subrace != null ? subrace.asiChoiceCount() : 0;
    }

    public static Map<String, Integer> fixedBonuses(String raceKey, String subraceKey) {
        Subrace subrace = getSubrace(raceKey, subraceKey);
        return subrace != null ? subrace.asiBoosts() : Map.of();
    }

    public static List<String> traits(String raceKey, String subraceKey) {
        Subrace subrace = getSubrace(raceKey, subraceKey);
        return subrace != null ? subrace.traits() : List.of();
    }

    /** Applies the fixed racial bonuses, then each flexible choice (+1 each). */
    public static Map<String, Integer> totalBonuses(String raceKey, String subraceKey,
                                                    List<String> choices) {
        Map<String, Integer> result = new LinkedHashMap<>(fixedBonuses(raceKey, subraceKey));
        if (choices != null) {
            for (String ability : choices) {
                result.merge(ability, 1, Integer::sum);
            }
        }
        return result;
    }

    /** JSON for the creation page (all display text is resolved through translation keys). */
    public static String toJson() {
        Gson gson = new GsonBuilder().create();
        JsonObject root = new JsonObject();
        JsonArray races = new JsonArray();
        for (Map.Entry<String, RaceEntry> entry : RACES.entrySet()) {
            RaceEntry race = entry.getValue();
            JsonObject raceJson = new JsonObject();
            raceJson.addProperty("key", entry.getKey());
            raceJson.addProperty("speed", race.speed());
            raceJson.addProperty("size", race.size());
            JsonArray subraces = new JsonArray();
            for (Subrace subrace : race.subraces()) {
                JsonObject subraceJson = new JsonObject();
                subraceJson.addProperty("key", subrace.key());
                subraceJson.addProperty("asiChoice", subrace.asiChoiceCount());
                subraceJson.addProperty("speed", subrace.speed() > 0 ? subrace.speed() : race.speed());
                JsonObject asi = new JsonObject();
                for (Map.Entry<String, Integer> boost : subrace.asiBoosts().entrySet()) {
                    asi.addProperty(boost.getKey(), boost.getValue());
                }
                subraceJson.add("asi", asi);
                JsonArray traits = new JsonArray();
                for (String trait : subrace.traits()) {
                    traits.add(trait);
                }
                subraceJson.add("traits", traits);
                subraces.add(subraceJson);
            }
            raceJson.add("subraces", subraces);
            races.add(raceJson);
        }
        root.add("races", races);
        return gson.toJson(root);
    }
}
