package mdh.dndclasses.feature.effect;

import com.google.gson.JsonObject;
import mdh.dndclasses.feature.EffectDefinition;
import mdh.dndclasses.feature.EffectHandler;
import mdh.dndclasses.feature.FeatureDefinition;
import mdh.dndclasses.feature.Scaling;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Instantaneous heal. Only meaningful on active features (fired from {@link #use}); it never
 * applies passively, so it is a no-op in {@link #apply}/{@link #remove}.
 * <p>
 * params: {@code { "type": "dndclasses:heal", "amount": { "dice": "1d10", "per_level": 1 } }}
 * The amount is a {@link Scaling} value (dice are converted to their expectation).
 */
public class HealEffectHandler implements EffectHandler {

    private static final ResourceLocation TYPE = new ResourceLocation("dndclasses", "heal");

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
        if (params == null || !params.has("amount")) {
            return;
        }
        double amount = Scaling.eval(player, params.get("amount"));
        if (amount > 0) {
            player.heal((float) amount);
        }
    }
}
