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
        String name,
        String nameKey,
        String description,
        String descriptionKey,
        List<EffectDefinition> effects
) {
    public enum OwnerType {CLASS, SUBCLASS}
    public enum Kind { PASSIVE, ACTIVE }
    public enum Rest { NONE, SHORT, LONG }
}

