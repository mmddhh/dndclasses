package mdh.dndclasses.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mdh.dndclasses.ability.Iability;
import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.feature.CharacterRefresh;
import mdh.dndclasses.level.Ilevel;
import mdh.dndclasses.network.ServerStateSync;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Applies an Ability Score Improvement: +2 to one score, or +1 to two different scores. */
public final class AsiSelection {

    private AsiSelection() {
    }

    public static boolean apply(ServerPlayer player, JsonObject payload) {
        Ilevel level = player.getCapability(ModCapabilities.LEVEL_CAPABILITY).orElse(null);
        if (level == null || level.getPendingAsi() <= 0) {
            return false;
        }
        String mode = payload.has("mode") ? payload.get("mode").getAsString() : "";
        List<String> abilities = new java.util.ArrayList<>();
        if (payload.has("abilities") && payload.get("abilities").isJsonArray()) {
            for (com.google.gson.JsonElement element : payload.getAsJsonArray("abilities")) {
                abilities.add(element.getAsString());
            }
        }
        int[] increments = switch (mode) {
            case "plus2" -> abilities.size() == 1 ? new int[]{2} : null;
            case "plus1plus1" -> abilities.size() == 2 && !abilities.get(0).equals(abilities.get(1))
                    ? new int[]{1, 1} : null;
            default -> null;
        };
        if (increments == null) {
            player.sendSystemMessage(Component.literal("属性提升选择不合法。"));
            return false;
        }
        for (String ability : abilities) {
            if (!RaceData.ABILITIES.contains(ability)) {
                return false;
            }
        }
        Iability data = player.getCapability(ModCapabilities.ABILITY_CAPABILITY).orElse(null);
        if (data == null) {
            return false;
        }
        for (int i = 0; i < abilities.size(); i++) {
            add(data, abilities.get(i), increments[i]);
        }
        level.setPendingAsi(level.getPendingAsi() - 1);
        CharacterRefresh.refresh(player);
        ServerStateSync.send(player);
        player.sendSystemMessage(Component.literal("属性提升已应用。"));
        ChoiceManager.openPending(player);
        return true;
    }

    public static boolean applyJson(ServerPlayer player, String json) {
        try {
            return apply(player, JsonParser.parseString(json).getAsJsonObject());
        } catch (Exception exception) {
            return false;
        }
    }

    private static void add(Iability data, String ability, int amount) {
        switch (ability) {
            case "str" -> data.setstr(Math.min(20, data.getstr() + amount));
            case "dex" -> data.setdex(Math.min(20, data.getdex() + amount));
            case "con" -> data.setcon(Math.min(20, data.getcon() + amount));
            case "int" -> data.setint(Math.min(20, data.getint() + amount));
            case "wis" -> data.setwis(Math.min(20, data.getwis() + amount));
            case "cha" -> data.setcha(Math.min(20, data.getcha() + amount));
            default -> {
            }
        }
    }
}
