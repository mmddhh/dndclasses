package mdh.dndclasses.data;

import java.util.LinkedHashMap;
import java.util.Map;

public class RaceRegistry {
    public static final Map<String, RaceInfo> RACES = new LinkedHashMap<>();

    static {
        // PHB races
        RACES.put("dwarf",       new RaceInfo("dwarf",       25, "medium", 30));
        RACES.put("elf",         new RaceInfo("elf",         25, "medium", 30));
        RACES.put("halfling",    new RaceInfo("halfling",    25, "small",  25));
        RACES.put("human",       new RaceInfo("human",       25, "medium", 30));
        RACES.put("dragonborn",  new RaceInfo("dragonborn",  25, "medium", 30));
        RACES.put("gnome",       new RaceInfo("gnome",       25, "small",  25));
        RACES.put("half_elf",    new RaceInfo("half_elf",    25, "medium", 30));
        RACES.put("half_orc",    new RaceInfo("half_orc",    25, "medium", 30));
        RACES.put("tiefling",    new RaceInfo("tiefling",    25, "medium", 30));
    }

    public record RaceInfo(String key, int speed, String size, int age) {}

    public static boolean isValid(String key) {
        return RACES.containsKey(key);
    }
}
