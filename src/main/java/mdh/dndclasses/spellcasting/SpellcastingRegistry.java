package mdh.dndclasses.spellcasting;

import mdh.dndclasses.feature.FeatureDefinition;
import mdh.dndclasses.spells.Preparation;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class SpellcastingRegistry {

    private static final Map<ResourceLocation, Map<Integer, SpellLevelEntry>> BY_OWNER = new HashMap<>();
    private static final Map<ResourceLocation, Preparation> PREPARATION = new HashMap<>();
    private static final Map<ResourceLocation, String> ABILITY = new HashMap<>();
    private static final Map<ResourceLocation, Boolean> SHORT_REST_RECOVERY = new HashMap<>();
    private static final Map<ResourceLocation, FeatureDefinition.OwnerType> OWNER_TYPE = new HashMap<>();

    private SpellcastingRegistry() {
    }

    public static void clear() {
        BY_OWNER.clear();
        PREPARATION.clear();
        ABILITY.clear();
        SHORT_REST_RECOVERY.clear();
        OWNER_TYPE.clear();
    }

    public static void register(ResourceLocation owner, Preparation preparation, String ability,
                                boolean shortRestRecovery, FeatureDefinition.OwnerType ownerType,
                                Map<Integer, SpellLevelEntry> byLevel) {
        BY_OWNER.put(owner, byLevel);
        PREPARATION.put(owner, preparation == null ? Preparation.NONE : preparation);
        ABILITY.put(owner, ability == null ? "" : ability);
        SHORT_REST_RECOVERY.put(owner, shortRestRecovery);
        OWNER_TYPE.put(owner, ownerType == null ? FeatureDefinition.OwnerType.CLASS : ownerType);
    }

    public static boolean has(ResourceLocation owner) {
        return BY_OWNER.containsKey(owner);
    }

    public static Preparation getPreparation(ResourceLocation owner) {
        return PREPARATION.getOrDefault(owner, Preparation.NONE);
    }

    /** Casting ability key ("int"/"wis"/"cha") used to scale prepared-spell limits; empty when unset. */
    public static String getAbility(ResourceLocation owner) {
        return ABILITY.getOrDefault(owner, "");
    }

    /** Whether this owner regains spell slots on a short rest (warlock pact magic). */
    public static boolean isShortRestRecovery(ResourceLocation owner) {
        return SHORT_REST_RECOVERY.getOrDefault(owner, false);
    }

    public static FeatureDefinition.OwnerType getOwnerType(ResourceLocation owner) {
        return OWNER_TYPE.getOrDefault(owner, FeatureDefinition.OwnerType.CLASS);
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
