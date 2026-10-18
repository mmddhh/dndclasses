package mdh.dndclasses.conditions;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConditionRegistry {

    private static final Map<ResourceLocation, ConditionDefinition> BY_ID = new LinkedHashMap<>();

    private ConditionRegistry() {
    }

    public static void clear() {
        BY_ID.clear();
    }

    public static void register(ConditionDefinition definition) {
        BY_ID.put(definition.id(), definition);
    }

    public static ConditionDefinition get(ResourceLocation id) {
        return BY_ID.get(id);
    }

    public static Collection<ConditionDefinition> all() {
        return new ArrayList<>(BY_ID.values());
    }
}
