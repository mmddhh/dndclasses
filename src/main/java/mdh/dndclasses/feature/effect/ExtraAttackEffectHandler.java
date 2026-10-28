package mdh.dndclasses.feature.effect;

import mdh.dndclasses.feature.EffectDefinition;
import mdh.dndclasses.feature.EffectHandler;
import mdh.dndclasses.feature.FeatureDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Passive "extra attack" marker. The value is read at attack time by
 * {@link mdh.dndclasses.feature.FeatureEffects#extraAttacks(ServerPlayer)}, so this handler keeps no
 * state of its own.
 * <p>
 * params: {@code { "type": "dndclasses:extra_attack", "attacks": 1 }}
 */
public class ExtraAttackEffectHandler implements EffectHandler {

    private static final ResourceLocation TYPE = new ResourceLocation("dndclasses", "extra_attack");

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
}
