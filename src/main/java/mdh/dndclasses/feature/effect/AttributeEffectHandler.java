package mdh.dndclasses.feature.effect;

import com.google.gson.JsonObject;
import mdh.dndclasses.feature.EffectDefinition;
import mdh.dndclasses.feature.EffectHandler;
import mdh.dndclasses.feature.FeatureDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Declarative attribute modifier effect.
 * params: { "attribute": "minecraft:generic.movement_speed", "amount": 0.0333, "operation": "addition" }
 * The modifier UUID is derived from the feature id + effect index, so authors never write UUIDs.
 */
public class AttributeEffectHandler implements EffectHandler {

    private static final ResourceLocation TYPE = new ResourceLocation("dndclasses", "attribute");

    @Override
    public ResourceLocation type() {
        return TYPE;
    }

    @Override
    public void apply(ServerPlayer player, FeatureDefinition feature, EffectDefinition effect) {
        JsonObject params = effect.params();
        if (params == null || !params.has("attribute")) {
            return;
        }

        ResourceLocation attributeId = ResourceLocation.tryParse(params.get("attribute").getAsString());
        if (attributeId == null) {
            return;
        }
        Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(attributeId);
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        AttributeModifier modifier = new AttributeModifier(
                modifierId(feature, effect),
                feature.id().toString(),
                params.get("amount").getAsDouble(),
                operation(params.has("operation") ? params.get("operation").getAsString() : "addition"));

        if (!instance.hasModifier(modifier)) {
            instance.addTransientModifier(modifier);
        }
    }

    @Override
    public void remove(ServerPlayer player, FeatureDefinition feature, EffectDefinition effect) {
        JsonObject params = effect.params();
        if (params == null || !params.has("attribute")) {
            return;
        }

        ResourceLocation attributeId = ResourceLocation.tryParse(params.get("attribute").getAsString());
        if (attributeId == null) {
            return;
        }
        Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(attributeId);
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        instance.removeModifier(modifierId(feature, effect));
    }

    private static UUID modifierId(FeatureDefinition feature, EffectDefinition effect) {
        return UUID.nameUUIDFromBytes(
                (feature.id() + "#" + effect.index()).getBytes(StandardCharsets.UTF_8));
    }

    private static AttributeModifier.Operation operation(String raw) {
        if ("multiply_base".equalsIgnoreCase(raw)) {
            return AttributeModifier.Operation.MULTIPLY_BASE;
        }
        if ("multiply_total".equalsIgnoreCase(raw)) {
            return AttributeModifier.Operation.MULTIPLY_TOTAL;
        }
        return AttributeModifier.Operation.ADDITION;
    }
}
