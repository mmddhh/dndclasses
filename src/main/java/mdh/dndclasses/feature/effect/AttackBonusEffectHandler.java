package mdh.dndclasses.feature.effect;

import mdh.dndclasses.feature.EffectDefinition;
import mdh.dndclasses.feature.EffectHandler;
import mdh.dndclasses.feature.FeatureDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Passive damage bonus on weapon attacks. Read at attack time by
 * {@link mdh.dndclasses.feature.FeatureEffects#attackBonus(ServerPlayer)}.
 * <p>
 * params: {@code { "type": "dndclasses:attack_bonus", "amount": 2 }}
 */
public class AttackBonusEffectHandler implements EffectHandler {

    private static final ResourceLocation TYPE = new ResourceLocation("dndclasses", "attack_bonus");

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
