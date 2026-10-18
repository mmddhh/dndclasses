package mdh.dndclasses.feature;

import mdh.dndclasses.feature.effect.AttributeEffectHandler;
import mdh.dndclasses.feature.effect.HealEffectHandler;
import mdh.dndclasses.feature.effect.StatusEffectHandler;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class EffectRegistry {

    private static final Map<ResourceLocation, EffectHandler> HANDLERS = new HashMap<>();

    private EffectRegistry() {
    }

    public static void registerDefaults() {
        register(new AttributeEffectHandler());
        register(new HealEffectHandler());
        register(new StatusEffectHandler());
    }

    public static void register(EffectHandler handler) {
        HANDLERS.put(handler.type(), handler);
    }

    public static EffectHandler get(ResourceLocation type) {
        return HANDLERS.get(type);
    }
}
