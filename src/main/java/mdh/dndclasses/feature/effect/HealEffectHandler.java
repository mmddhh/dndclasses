package mdh.dndclasses.feature.effect;

import mdh.dndclasses.feature.EffectDefinition;
import mdh.dndclasses.feature.EffectHandler;
import mdh.dndclasses.feature.FeatureDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class HealEffectHandler implements EffectHandler {
    @Override
    public ResourceLocation type() {
        return null;
    }

    @Override
    public void apply(ServerPlayer player, FeatureDefinition feature, EffectDefinition effect) {

    }

    @Override
    public void remove(ServerPlayer player, FeatureDefinition feature, EffectDefinition effect) {

    }
}
