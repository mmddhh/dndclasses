package mdh.dndclasses.feature;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Executes one declarative effect type. Implementations must be idempotent. */
public interface EffectHandler {

    ResourceLocation type();

    void apply(ServerPlayer player, FeatureDefinition feature, EffectDefinition effect);

    void remove(ServerPlayer player, FeatureDefinition feature, EffectDefinition effect);

    /** Called when an active feature is used. Passive effects ignore this. */
    default void use(ServerPlayer player, FeatureDefinition feature, EffectDefinition effect) {
    }
}
