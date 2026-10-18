package mdh.dndclasses.feature;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record FeatureDefinition(
        ResourceLocation id,
        OwnerType ownerType,
        ResourceLocation owner,
        int level,
        Kind kind,
        Rest rest,
        int maxUses,
        List<Integer> maxUsesByLevel,
        String name,
        String nameKey,
        String description,
        String descriptionKey,
        List<EffectDefinition> effects
) {
    public enum OwnerType {CLASS, SUBCLASS}
    public enum Kind { PASSIVE, ACTIVE }
    public enum Rest { NONE, SHORT, LONG }

    /** Max uses at the given player level: per-level table when present, else the flat {@link #maxUses}. */
    public int usesAtLevel(int playerLevel) {
        if (maxUsesByLevel == null || maxUsesByLevel.isEmpty()) {
            return Math.max(1, maxUses);
        }
        int index = Math.max(0, Math.min(maxUsesByLevel.size() - 1, playerLevel - 1));
        return Math.max(1, maxUsesByLevel.get(index));
    }
}

