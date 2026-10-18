package mdh.dndclasses.conditions;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import mdh.dndclasses.feature.CharacterRefresh;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/** Loads condition definitions from {@code data/<namespace>/dndclasses/conditions/*.json}. */
public class ConditionReloadListener extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();

    public ConditionReloadListener() {
        super(new Gson(), "dndclasses/conditions");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        ConditionRegistry.clear();
        int loaded = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation file = entry.getKey();
            try {
                JsonObject root = entry.getValue().getAsJsonObject();
                ResourceLocation id = parseId(requiredString(root, "id"), file.getNamespace());
                String nameKey = optionalString(root, "name_key", "condition." + id.getNamespace() + "." + id.getPath());
                String descriptionKey = optionalString(root, "description_key", nameKey + ".desc");
                String saveAbility = optionalString(root, "save_ability", "");
                int saveDc = optionalInt(root, "save_dc", 10);
                int saveInterval = optionalInt(root, "save_interval_ticks", 80);
                int duration = optionalInt(root, "duration_ticks", 600);

                EnumSet<ConditionFlag> flags = EnumSet.noneOf(ConditionFlag.class);
                if (root.has("flags")) {
                    JsonArray array = root.getAsJsonArray("flags");
                    for (int i = 0; i < array.size(); i++) {
                        try {
                            flags.add(ConditionFlag.valueOf(array.get(i).getAsString().toUpperCase(java.util.Locale.ROOT)));
                        } catch (IllegalArgumentException exception) {
                            LOGGER.warn("Unknown condition flag '{}' in {}", array.get(i).getAsString(), file);
                        }
                    }
                }

                List<ConditionEffect> effects = new ArrayList<>();
                if (root.has("status_effects")) {
                    JsonArray array = root.getAsJsonArray("status_effects");
                    for (int i = 0; i < array.size(); i++) {
                        JsonObject o = array.get(i).getAsJsonObject();
                        ResourceLocation effect = parseId(requiredString(o, "effect"), "minecraft");
                        effects.add(new ConditionEffect(effect,
                                optionalInt(o, "amplifier", 0),
                                o.has("ambient") && o.get("ambient").getAsBoolean(),
                                !o.has("show_particles") || o.get("show_particles").getAsBoolean(),
                                !o.has("show_icon") || o.get("show_icon").getAsBoolean()));
                    }
                }

                List<AttributeModifierSpec> modifiers = new ArrayList<>();
                if (root.has("attribute_modifiers")) {
                    JsonArray array = root.getAsJsonArray("attribute_modifiers");
                    for (int i = 0; i < array.size(); i++) {
                        JsonObject o = array.get(i).getAsJsonObject();
                        ResourceLocation attribute = parseId(requiredString(o, "attribute"), "minecraft");
                        double amount = o.get("amount").getAsDouble();
                        AttributeModifier.Operation operation = operation(optionalString(o, "operation", "addition"));
                        modifiers.add(new AttributeModifierSpec(attribute, amount, operation));
                    }
                }

                ConditionRegistry.register(new ConditionDefinition(id, nameKey, descriptionKey, saveAbility,
                        saveDc, saveInterval, duration, flags, effects, modifiers));
                loaded++;
            } catch (Exception exception) {
                LOGGER.error("Failed to load condition from {}", file, exception);
            }
        }

        LOGGER.info("Loaded {} condition(s) from {} file(s)", loaded, entries.size());

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    CharacterRefresh.refresh(player);
                }
            });
        }
    }

    private static ResourceLocation parseId(String raw, String sourceNamespace) {
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

    private static int optionalInt(JsonObject object, String key, int fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        return object.get(key).getAsInt();
    }

    private static AttributeModifier.Operation operation(String raw) {
        return switch (raw.toLowerCase(java.util.Locale.ROOT)) {
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> AttributeModifier.Operation.ADDITION;
        };
    }
}
