package mdh.dndclasses.conditions;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** One attribute modifier applied while a condition is active. */
public record AttributeModifierSpec(ResourceLocation attribute, double amount, AttributeModifier.Operation operation) {
}
