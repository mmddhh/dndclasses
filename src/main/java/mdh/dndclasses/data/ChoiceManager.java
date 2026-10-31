package mdh.dndclasses.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mdh.dndclasses.ability.Iability;
import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.level.Ilevel;
import mdh.dndclasses.network.DndNetwork;
import mdh.dndclasses.network.OpenChoicePacket;
import net.minecraft.server.level.ServerPlayer;

/** Decides which selection page (if any) must be shown to a player and builds its payload. */
public final class ChoiceManager {

    public static final String SUBCLASS = "subclass";
    public static final String ASI = "asi";
    public static final String CLOSE = "close";

    private ChoiceManager() {
    }

    /** Pending ASI first (it is a blocking choice), then the subclass, otherwise close the page. */
    public static void openPending(ServerPlayer player) {
        Ilevel level = player.getCapability(ModCapabilities.LEVEL_CAPABILITY).orElse(null);
        if (level == null || !level.hasCreatedCharacter()) {
            return;
        }
        if (level.getPendingAsi() > 0) {
            DndNetwork.sendToPlayer(player, new OpenChoicePacket(asiPayload(player, level)));
            return;
        }
        if (needsSubclass(player, level)) {
            DndNetwork.sendToPlayer(player, new OpenChoicePacket(subclassPayload(level)));
            return;
        }
        JsonObject close = new JsonObject();
        close.addProperty("choice", CLOSE);
        DndNetwork.sendToPlayer(player, new OpenChoicePacket(close.toString()));
    }

    public static boolean needsSubclass(ServerPlayer player, Ilevel level) {
        if (level.getSubclassKey() != null && !level.getSubclassKey().isEmpty()) {
            return false;
        }
        SubclassRegistry.ClassSubclasses entry = SubclassRegistry.get(level.getclassName());
        return entry != null && level.getlevel() >= entry.selectLevel();
    }

    private static String asiPayload(ServerPlayer player, Ilevel level) {
        JsonObject root = new JsonObject();
        root.addProperty("choice", ASI);
        root.addProperty("pending", level.getPendingAsi());
        root.addProperty("class", level.getclassName());
        JsonObject abilities = new JsonObject();
        player.getCapability(ModCapabilities.ABILITY_CAPABILITY).ifPresent(data -> {
            abilities.addProperty("str", data.getstr());
            abilities.addProperty("dex", data.getdex());
            abilities.addProperty("con", data.getcon());
            abilities.addProperty("int", data.getint());
            abilities.addProperty("wis", data.getwis());
            abilities.addProperty("cha", data.getcha());
        });
        root.add("abilities", abilities);
        return root.toString();
    }

    private static String subclassPayload(Ilevel level) {
        JsonObject root = new JsonObject();
        root.addProperty("choice", SUBCLASS);
        root.addProperty("class", level.getclassName());
        root.addProperty("level", level.getlevel());
        JsonArray options = new JsonArray();
        SubclassRegistry.ClassSubclasses entry = SubclassRegistry.get(level.getclassName());
        if (entry != null) {
            for (String id : entry.ids()) {
                JsonObject option = new JsonObject();
                option.addProperty("id", id);
                option.addProperty("name", SubclassSelection.displayName(id));
                options.add(option);
            }
        }
        root.add("options", options);
        return root.toString();
    }
}
