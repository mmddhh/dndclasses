package mdh.dndclasses.spellcasting;

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
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads spellcasting tables from {@code data/<namespace>/dndclasses/spellcasting/*.json}.
 * The file's namespace is the only namespace its owner id may use.
 */
public class SpellcastingReloadListener extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();

    public SpellcastingReloadListener() {
        super(new Gson(), "dndclasses/spellcasting");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        SpellcastingRegistry.clear();

        int loaded = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation file = entry.getKey();
            String sourceNamespace = file.getNamespace();
            try {
                JsonObject root = entry.getValue().getAsJsonObject();
                ResourceLocation owner = parseId(root.get("owner").getAsString(), sourceNamespace);
                JsonArray levels = root.getAsJsonArray("levels");

                Map<Integer, SpellLevelEntry> byLevel = new HashMap<>();
                for (int i = 0; i < levels.size(); i++) {
                    byLevel.put(levels.get(i).getAsJsonObject().get("level").getAsInt(),
                            parseLevel(levels.get(i).getAsJsonObject()));
                }
                SpellcastingRegistry.register(owner, byLevel);
                loaded++;
            } catch (Exception exception) {
                LOGGER.error("Failed to load spellcasting table from {}", file, exception);
            }
        }

        LOGGER.info("Loaded {} spellcasting table(s) from {} file(s)", loaded, entries.size());

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    CharacterRefresh.refresh(player);
                }
            });
        }
    }

    private static SpellLevelEntry parseLevel(JsonObject object) {
        int level = object.get("level").getAsInt();
        int[] slots = new int[9];
        JsonArray slotArray = object.getAsJsonArray("slots");
        for (int ring = 0; ring < 9 && ring < slotArray.size(); ring++) {
            slots[ring] = slotArray.get(ring).getAsInt();
        }
        int knownSpells = object.has("known_spells") ? object.get("known_spells").getAsInt() : 0;
        int knownCantrips = object.has("known_cantrips") ? object.get("known_cantrips").getAsInt() : 0;
        return new SpellLevelEntry(level, slots, knownSpells, knownCantrips);
    }

    private static ResourceLocation parseId(String raw, String sourceNamespace) {
        if (raw == null || raw.indexOf(':') <= 0) {
            throw new IllegalArgumentException("invalid id, must be namespace:path: " + raw);
        }
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            throw new IllegalArgumentException("malformed id: " + raw);
        }
        if (!id.getNamespace().equals(sourceNamespace)) {
            throw new IllegalArgumentException("owner id namespace must match the data pack namespace: " + id);
        }
        return id;
    }
}
