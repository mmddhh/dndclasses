package mdh.dndclasses.feature;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FeatureRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, FeatureDefinition> BY_ID = new HashMap<>();
    private static final Map<ResourceLocation, List<FeatureDefinition>> BY_OWNER = new HashMap<>();

    private FeatureRegistry() {
    }

    public static void clear() {
        BY_ID.clear();
        BY_OWNER.clear();
    }

    /** @param sourceNamespace 数据包文件的命名空间（从文件 ResourceLocation 取） */
    public static boolean register(FeatureDefinition def, String sourceNamespace) {
        if (def == null) {
            LOGGER.error("FeatureDefinition 为 null");
            return false;
        }
        if (def.id() == null || def.owner() == null) {
            LOGGER.error("FeatureDefinition 缺少 id 或 owner");
            return false;
        }
        if (sourceNamespace == null || sourceNamespace.isEmpty()) {
            LOGGER.error("命名空间为空{}", def.id());
            return false;
        }
        if (sourceNamespace.equals("minecraft") || sourceNamespace.equals("forge")) {
            LOGGER.error("命名空间不能使用minecraft或者forge{}", def.id());
            return false;
        }
        String ns = def.id().getNamespace();
        if (!ns.equals(sourceNamespace)) {
            LOGGER.error("feature id namespace must match the data pack namespace: id={} pack={}", def.id(), sourceNamespace);
            return false;
        }
        if (BY_ID.containsKey(def.id())) {
            LOGGER.error("与已有特性名称重复: {}", def.id());
            return false;
        }
        BY_ID.put(def.id(), def);
        BY_OWNER.computeIfAbsent(def.owner(), k -> new ArrayList<>()).add(def);
        return true;
    }

    public static FeatureDefinition get(ResourceLocation id) {
        return BY_ID.get(id);
    }

    public static List<FeatureDefinition> byOwner(ResourceLocation owner) {
        return BY_OWNER.getOrDefault(owner, List.of());
    }
}