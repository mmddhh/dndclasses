package mdh.dndclasses.feature.effect;

import com.google.gson.JsonObject;
import mdh.dndclasses.feature.EffectDefinition;
import mdh.dndclasses.feature.EffectHandler;
import mdh.dndclasses.feature.FeatureDefinition;
import mdh.dndclasses.feature.Scaling;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Instantaneous damage against the entity the player is looking at (active features only).
 * <p>
 * params: {@code { "type": "dndclasses:damage", "amount": { "dice": "2d8" }, "range": 4 }}
 * The amount is a {@link Scaling} value; {@code range} is in blocks (default 4).
 */
public class DamageEffectHandler implements EffectHandler {

    private static final ResourceLocation TYPE = new ResourceLocation("dndclasses", "damage");

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
        if (amount <= 0) {
            return;
        }
        double range = params.has("range") ? params.get("range").getAsDouble() : 4.0D;
        HitResult hit = player.pick(range, 0.0F, false);
        if (!(hit instanceof EntityHitResult entityHit)) {
            return;
        }
        Entity target = entityHit.getEntity();
        if (!(target instanceof LivingEntity living) || living == player || living.isDeadOrDying()) {
            return;
        }
        living.hurt(player.damageSources().playerAttack(player), (float) amount);
    }
}
