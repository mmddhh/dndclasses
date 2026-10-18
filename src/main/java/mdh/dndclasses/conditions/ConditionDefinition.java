package mdh.dndclasses.conditions;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

/** Data-driven definition of one condition (loaded from dndclasses/conditions/*.json). */
public record ConditionDefinition(
        ResourceLocation id,
        String nameKey,
        String descriptionKey,
        String saveAbility,
        int saveDc,
        int saveIntervalTicks,
        int durationTicks,
        Set<ConditionFlag> flags,
        List<ConditionEffect> effects,
        List<AttributeModifierSpec> modifiers
) {
    public ConditionDefinition {
        flags = Set.copyOf(flags);
        effects = List.copyOf(effects);
        modifiers = List.copyOf(modifiers);
    }

    public boolean has(ConditionFlag flag) {
        return flags.contains(flag);
    }

    public boolean canSave() {
        return saveAbility != null && !saveAbility.isEmpty();
    }
}
