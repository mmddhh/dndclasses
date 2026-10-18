package mdh.dndclasses.conditions;

import net.minecraft.resources.ResourceLocation;

/** One mob effect applied while a condition is active. */
public record ConditionEffect(ResourceLocation effect, int amplifier, boolean ambient, boolean showParticles, boolean showIcon) {
}
