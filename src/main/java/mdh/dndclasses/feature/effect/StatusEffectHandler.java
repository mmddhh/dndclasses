package mdh.dndclasses.feature.effect;

import com.google.gson.JsonObject;
import mdh.dndclasses.feature.EffectDefinition;
import mdh.dndclasses.feature.EffectHandler;
import mdh.dndclasses.feature.FeatureDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Grants/removes a mob effect.
 * <ul>
 *   <li>{@code use()} (active feature): timed buff using {@code duration} ticks (a missing or
 *       non-positive duration means infinite).</li>
 *   <li>{@code apply()}/{@code remove()} (passive feature): an infinite aura, added on apply and
 *       removed on remove.</li>
 * </ul>
 * params: {@code { "type": "dndclasses:status", "effect": "minecraft:strength", "duration": 100,
 * "amplifier": 0, "ambient": false, "show_particles": false, "show_icon": true }}
 */
public class StatusEffectHandler implements EffectHandler {

    private static final ResourceLocation TYPE = new ResourceLocation("dndclasses", "status");
    private static final int INFINITE = -1;

    @Override
    public ResourceLocation type() {
        return TYPE;
    }

    @Override
    public void apply(ServerPlayer player, FeatureDefinition feature, EffectDefinition effect) {
        MobEffect mobEffect = resolve(effect.params());
        if (mobEffect == null) {
            return;
        }
        player.removeEffect(mobEffect);
        player.addEffect(build(mobEffect, effect.params(), INFINITE));
    }

    @Override
    public void remove(ServerPlayer player, FeatureDefinition feature, EffectDefinition effect) {
        MobEffect mobEffect = resolve(effect.params());
        if (mobEffect != null) {
            player.removeEffect(mobEffect);
        }
    }

    @Override
    public void use(ServerPlayer player, FeatureDefinition feature, EffectDefinition effect) {
        MobEffect mobEffect = resolve(effect.params());
        if (mobEffect == null) {
            return;
        }
        player.addEffect(build(mobEffect, effect.params(), duration(effect.params())));
    }

    private static MobEffect resolve(JsonObject params) {
        if (params == null || !params.has("effect")) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(params.get("effect").getAsString());
        return id == null ? null : BuiltInRegistries.MOB_EFFECT.get(id);
    }

    private static int duration(JsonObject params) {
        if (params == null || !params.has("duration")) {
            return INFINITE;
        }
        int value = params.get("duration").getAsInt();
        return value > 0 ? value : INFINITE;
    }

    private static MobEffectInstance build(MobEffect mobEffect, JsonObject params, int duration) {
        int amplifier = params != null && params.has("amplifier") ? params.get("amplifier").getAsInt() : 0;
        boolean ambient = params != null && params.has("ambient") && params.get("ambient").getAsBoolean();
        boolean showParticles = params == null || !params.has("show_particles") || params.get("show_particles").getAsBoolean();
        boolean showIcon = params == null || !params.has("show_icon") || params.get("show_icon").getAsBoolean();
        return new MobEffectInstance(mobEffect, duration, amplifier, ambient, showParticles, showIcon);
    }
}
