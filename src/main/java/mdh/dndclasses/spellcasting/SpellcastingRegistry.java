package mdh.dndclasses.spellcasting;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class SpellcastingRegistry {

    private static final Map<ResourceLocation, Map<Integer, SpellLevelEntry>> BY_OWNER = new HashMap<>();

    private SpellcastingRegistry() {
    }

    public static void clear() {
        BY_OWNER.clear();
    }

    public static void register(ResourceLocation owner, Map<Integer, SpellLevelEntry> byLevel) {
        BY_OWNER.put(owner, byLevel);
    }

    public static boolean has(ResourceLocation owner) {
        return BY_OWNER.containsKey(owner);
    }

    /** The entry with the highest level &lt;= the given player level. */
    public static SpellLevelEntry get(ResourceLocation owner, int level) {
        Map<Integer, SpellLevelEntry> byLevel = BY_OWNER.get(owner);
        if (byLevel == null) {
            return null;
        }
        SpellLevelEntry best = null;
        for (Map.Entry<Integer, SpellLevelEntry> entry : byLevel.entrySet()) {
            if (entry.getKey() <= level && (best == null || entry.getKey() > best.level())) {
                best = entry.getValue();
            }
        }
        return best;
    }
}
