package mdh.dndclasses.client;

import com.google.gson.JsonParser;
import mdh.dndclasses.network.OpenChoicePacket;
import net.minecraft.client.Minecraft;

/** Opens (or closes) the AUI selection page requested by the server. */
public final class ChoiceScreenOpener {

    private static String json = "{}";

    private ChoiceScreenOpener() {
    }

    public static String json() {
        return json;
    }

    public static void open(OpenChoicePacket packet) {
        json = packet.json();
        String choice = "close";
        try {
            choice = JsonParser.parseString(json).getAsJsonObject()
                    .has("choice") ? JsonParser.parseString(json).getAsJsonObject()
                    .get("choice").getAsString() : "close";
        } catch (Exception ignored) {
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        switch (choice) {
            case "asi" -> minecraft.setScreen(new AsiScreen());
            case "subclass" -> minecraft.setScreen(new SubclassScreen());
            case "close" -> {
                if (minecraft.screen instanceof AsiScreen || minecraft.screen instanceof SubclassScreen) {
                    minecraft.setScreen(null);
                }
            }
            default -> {
            }
        }
    }
}
