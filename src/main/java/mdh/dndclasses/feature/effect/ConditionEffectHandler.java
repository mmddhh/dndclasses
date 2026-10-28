package mdh.dndclasses.feature.effect;

import com.google.gson.JsonObject;
import mdh.dndclasses.conditions.ConditionManager;
import mdh.dndclasses.feature.EffectDefinition;
import mdh.dndclasses.feature.EffectHandler;
import mdh.dndclasses.feature.FeatureDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Applies a D&amp;D condition to the player (active features only).
 * <p>
 * params: {@code { "type": "dndclasses:condition", "condition": "dndclasses:blinded",
 * "seconds": 10, "dc": 0 }}
 * Conditions are tracked per player, so this always targets the user; {@code dc} 0 means no save.
 */
public class ConditionEffectHandler implements EffectHandler {

    private static final ResourceLocation TYPE = new ResourceLocation("dndclasses", "condition");

    @Override
    public ResourceLocation type() {
        return TYPE;
    }

    @Override
    public void apply(ServerPlayer player, FeatureDefinition feature, EffectDefinition effect) {
    }

    @Override
    public void remove(ServerPlayer player, FeatureDefinition feature, EffectDefinition effect) {
    }

    @Override
    public void use(ServerPlayer player, FeatureDefinition feature, EffectDefinition effect) {
        JsonObject params = effect.params();
        if (params == null || !params.has("condition")) {
            return;
        }
        ResourceLocation condition = ResourceLocation.tryParse(params.get("condition").getAsString());
        if (condition == null) {
            return;
        }
        int seconds = params.has("seconds") ? params.get("seconds").getAsInt() : 10;
        int dc = params.has("dc") ? params.get("dc").getAsInt() : 0;
        ConditionManager.apply(player, condition, seconds * 20, player, dc);
    }
}
