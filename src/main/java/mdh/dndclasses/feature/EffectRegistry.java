package mdh.dndclasses.feature;

import mdh.dndclasses.feature.effect.AttributeEffectHandler;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class EffectRegistry {

    private static final Map<ResourceLocation, EffectHandler> HANDLERS = new HashMap<>();

    private EffectRegistry() {
    }

    public static void registerDefaults() {
        register(new AttributeEffectHandler());
    }

    public static void register(EffectHandler handler) {
        HANDLERS.put(handler.type(), handler);
    }

    public static EffectHandler get(ResourceLocation type) {
        return HANDLERS.get(type);
    }
}
