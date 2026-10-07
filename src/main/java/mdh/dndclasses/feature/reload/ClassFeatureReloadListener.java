package mdh.dndclasses.feature.reload;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import mdh.dndclasses.feature.EffectDefinition;
import mdh.dndclasses.feature.FeatureDefinition;
import mdh.dndclasses.feature.CharacterRefresh;
import mdh.dndclasses.feature.FeatureRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads class feature definitions from {@code data/<namespace>/dndclasses/class_features/*.json}
 * on first load and on every /reload. The file's namespace is the only namespace its
 * feature ids are allowed to use.
 */
public class ClassFeatureReloadListener extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();

    public ClassFeatureReloadListener() {
        super(new Gson(), "dndclasses/class_features");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        FeatureRegistry.clear();

        int loaded = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation file = entry.getKey();
            String sourceNamespace = file.getNamespace();
            try {
                JsonObject root = entry.getValue().getAsJsonObject();
                ResourceLocation owner = parseId(requiredString(root, "class"));
                JsonArray features = root.has("features") ? root.getAsJsonArray("features") : new JsonArray();
                for (int i = 0; i < features.size(); i++) {
                    FeatureDefinition definition = parseFeature(features.get(i).getAsJsonObject(), owner);
                    if (FeatureRegistry.register(definition, sourceNamespace)) {
                        loaded++;
                    }
                }
            } catch (Exception exception) {
                LOGGER.error("Failed to load class features from {}", file, exception);
            }
        }

        LOGGER.info("Loaded {} class feature(s) from {} file(s)", loaded, entries.size());

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    CharacterRefresh.refresh(player);
                }
            });
        }
    }

    private static FeatureDefinition parseFeature(JsonObject object, ResourceLocation owner) {
        ResourceLocation id = parseId(requiredString(object, "id"));
        int level = object.get("level").getAsInt();
        FeatureDefinition.Kind kind =
                "active".equalsIgnoreCase(optionalString(object, "kind", "passive"))
                        ? FeatureDefinition.Kind.ACTIVE
                        : FeatureDefinition.Kind.PASSIVE;
        FeatureDefinition.Rest rest = parseRest(optionalString(object, "rest", ""));
        int maxUses = object.has("max_uses") ? object.get("max_uses").getAsInt() : 0;

        return new FeatureDefinition(
                id,
                FeatureDefinition.OwnerType.CLASS,
                owner,
                level,
                kind,
                rest,
                maxUses,
                optionalString(object, "name", ""),
                optionalString(object, "name_key", ""),
                optionalString(object, "description", ""),
                optionalString(object, "description_key", ""),
                parseEffects(object));
    }

    private static List<EffectDefinition> parseEffects(JsonObject object) {
        List<EffectDefinition> effects = new ArrayList<>();
        if (!object.has("effects")) {
            return effects;
        }
        JsonArray array = object.getAsJsonArray("effects");
        for (int index = 0; index < array.size(); index++) {
            JsonObject effectObject = array.get(index).getAsJsonObject();
            ResourceLocation type = parseId(requiredString(effectObject, "type"));
            effects.add(new EffectDefinition(type, effectObject, index));
        }
        return effects;
    }

    private static FeatureDefinition.Rest parseRest(String raw) {
        if ("short".equalsIgnoreCase(raw)) {
            return FeatureDefinition.Rest.SHORT;
        }
        if ("long".equalsIgnoreCase(raw)) {
            return FeatureDefinition.Rest.LONG;
        }
        return FeatureDefinition.Rest.NONE;
    }

    private static ResourceLocation parseId(String raw) {
        if (raw == null || raw.indexOf(':') <= 0) {
            throw new IllegalArgumentException("invalid id, must be namespace:path: " + raw);
        }
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            throw new IllegalArgumentException("malformed id: " + raw);
        }
        return id;
    }

    private static String requiredString(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            throw new IllegalArgumentException("missing required field: " + key);
        }
        return object.get(key).getAsString();
    }

    private static String optionalString(JsonObject object, String key, String fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        return object.get(key).getAsString();
    }
}
