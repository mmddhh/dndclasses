package mdh.dndclasses.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sighs.apricityui.ApricityUI;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.network.DndNetwork;
import mdh.dndclasses.network.SaveQteResultPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Save / check QTE overlay (ApricityUI Overlay). The page animates the "judgement box + blocks"
 * (timing) or keycap + ring (mash) and writes {@code #qte-result} =
 * {@code {"results":[true|false|null,...],"done":bool}} whenever a block is judged. Java forwards
 * each newly resolved slot to the server immediately and closes once the page is done.
 */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SaveQteOverlay {

    private static final String PAGE = "hud/save_qte.html";
    private static final String DATA_ID = "qte-data";
    private static final String PRESS_ID = "qte-press";
    private static final String RESULT_ID = "qte-result";

    private static Document document;
    private static boolean open = false;
    private static String qteId = "";
    private static String lastPushed = "";
    private static String lastResult = "";
    private static boolean[] sent;

    private SaveQteOverlay() {
    }

    public static boolean isOpen() {
        return open;
    }

    /** From the server packet (client thread). */
    public static void open(String id, String json) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        qteId = id == null ? "" : id;
        String data = json == null ? "" : json;
        if (!open || document == null) {
            Document created = ApricityUI.createDocument(PAGE);
            if (created == null) {
                return;
            }
            document = created;
            open = true;
            lastPushed = "";
        }
        sent = null;
        lastResult = "";
        push(DATA_ID, data, true);

        Element keyElement = document.getElementById("qte-key");
        if (keyElement != null) {
            keyElement.setValue(ClientSetup.SAVE_QTE_KEY.getTranslatedKeyMessage().getString());
        }
    }

    public static void close() {
        if (document != null) {
            document.remove();
            document = null;
        }
        open = false;
        qteId = "";
        lastPushed = "";
        lastResult = "";
        sent = null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !open || document == null) {
            return;
        }
        if (Minecraft.getInstance().player == null) {
            close();
            return;
        }
        forwardResults();
    }

    private static void forwardResults() {
        Element resultElement = document.getElementById(RESULT_ID);
        if (resultElement == null) {
            return;
        }
        String raw = resultElement.getValue();
        if (raw == null || raw.isEmpty() || raw.equals(lastResult)) {
            return;
        }

        JsonObject object;
        try {
            object = JsonParser.parseString(raw).getAsJsonObject();
        } catch (Exception ignored) {
            return;
        }
        // stale results from a previous QTE are ignored until the page reports the current id
        if (!object.has("id") || !qteId.equals(object.get("id").getAsString())) {
            return;
        }
        lastResult = raw;

        JsonArray results = object.has("results") ? object.getAsJsonArray("results") : new JsonArray();
        if (sent == null || sent.length < results.size()) {
            boolean[] grown = new boolean[results.size()];
            if (sent != null) {
                System.arraycopy(sent, 0, grown, 0, sent.length);
            }
            sent = grown;
        }

        boolean allSent = results.size() > 0;
        for (int i = 0; i < results.size(); i++) {
            if (!sent[i] && !results.get(i).isJsonNull()) {
                boolean success = results.get(i).getAsBoolean();
                sent[i] = true;
                if (Minecraft.getInstance().getConnection() != null) {
                    DndNetwork.CHANNEL.sendToServer(new SaveQteResultPacket(qteId, i, success));
                }
            }
            if (!sent[i]) {
                allSent = false;
            }
        }

        boolean done = object.has("done") && object.get("done").getAsBoolean();
        if (done && allSent) {
            close();
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (!open || document == null || event.getAction() != 1) {
            return;
        }
        if (!ClientSetup.SAVE_QTE_KEY.matches(event.getKey(), event.getScanCode())) {
            return;
        }
        Element pressElement = document.getElementById(PRESS_ID);
        if (pressElement == null) {
            return;
        }
        int count;
        try {
            count = Integer.parseInt(pressElement.getValue());
        } catch (NumberFormatException exception) {
            count = 0;
        }
        pressElement.setValue(Integer.toString(count + 1));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        close();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            close();
        }
    }

    private static void push(String id, String value, boolean force) {
        if (document == null) {
            return;
        }
        Element element = document.getElementById(id);
        if (element == null) {
            return;
        }
        if (force || !value.equals(lastPushed)) {
            lastPushed = value;
            element.setValue(value);
        }
    }
}
