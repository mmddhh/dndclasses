package mdh.dndclasses.data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Canonical subclass ids per class, plus the level at which the subclass is chosen.
 * Phase 1 only stores/validates the choice; full feature derivation comes later.
 */
public final class SubclassRegistry {

    public record ClassSubclasses(int selectLevel, List<String> ids) {
        public boolean isValid(String id) {
            return id != null && ids.contains(id);
        }
    }

    public static final Map<String, ClassSubclasses> SUBCLASSES = new LinkedHashMap<>();

    static {
        SUBCLASSES.put("barbarian", at(3, "berserker", "totem_warrior"));
        SUBCLASSES.put("bard", at(3, "lore", "valor"));
        SUBCLASSES.put("cleric", at(1, "knowledge", "life", "light", "nature", "tempest", "trickery", "war"));
        SUBCLASSES.put("druid", at(2, "land", "moon"));
        SUBCLASSES.put("fighter", at(3, "champion", "battle_master", "eldritch_knight"));
        SUBCLASSES.put("monk", at(3, "open_hand", "shadow", "four_elements"));
        SUBCLASSES.put("paladin", at(3, "devotion", "ancients", "vengeance"));
        SUBCLASSES.put("ranger", at(3, "hunter", "beast_master"));
        SUBCLASSES.put("rogue", at(3, "thief", "assassin", "arcane_trickster"));
        SUBCLASSES.put("sorcerer", at(1, "draconic_bloodline", "wild_magic"));
        SUBCLASSES.put("warlock", at(1, "archfey", "fiend", "great_old_one"));
        SUBCLASSES.put("wizard", at(2, "abjuration", "conjuration", "divination", "enchantment",
                "evocation", "illusion", "necromancy", "transmutation"));
    }

    private SubclassRegistry() {
    }

    private static ClassSubclasses at(int selectLevel, String... keys) {
        java.util.ArrayList<String> ids = new java.util.ArrayList<>(keys.length);
        for (String key : keys) {
            ids.add("dndclasses:" + key);
        }
        return new ClassSubclasses(selectLevel, List.copyOf(ids));
    }

    public static ClassSubclasses get(String classKey) {
        return SUBCLASSES.get(classKey);
    }

    public static int getSelectLevel(String classKey) {
        ClassSubclasses entry = SUBCLASSES.get(classKey);
        return entry == null ? 3 : entry.selectLevel();
    }

    public static boolean isValid(String classKey, String subclassId) {
        ClassSubclasses entry = SUBCLASSES.get(classKey);
        return entry != null && entry.isValid(subclassId);
    }

    /** Selectable during level-1 character creation. */
    public static boolean isSelectableAtCreation(String classKey) {
        return getSelectLevel(classKey) <= 1;
    }
}
