package mdh.dndclasses.client;

import com.google.gson.JsonObject;
import com.sighs.apricityui.ApricityUI;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import mdh.dndclasses.ClientConfig;
import mdh.dndclasses.Dndclasses;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Stamina HUD. "aui" uses an ApricityUI overlay page (bottom left); "vanilla" draws a plain bar above
 * the hotbar; "off" hides it. The AUI page never blocks input and hides while a Screen is open.
 */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StaminaHudOverlay {

    private static final String PAGE = "hud/stamina.html";
    private static final String DATA_ID = "stamina-data";

    private static Document document;
    private static ClientConfig.StaminaHudMode mode;
    private static String lastData = "";

    private StaminaHudOverlay() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (Minecraft.getInstance().player == null) {
            closeAui();
            mode = null;
            return;
        }
        ensureMode();
        if (document != null) {
            pushData();
        }
    }

    private static void ensureMode() {
        ClientConfig.StaminaHudMode configured = ClientConfig.STAMINA_HUD.get();
        if (configured == mode) {
            return;
        }
        closeAui();
        mode = configured;
        lastData = "";
        if (mode == ClientConfig.StaminaHudMode.AUI) {
            Document created = ApricityUI.createDocument(PAGE);
            if (created != null) {
                document = created;
            }
        }
    }

    private static void pushData() {
        Element element = document.getElementById(DATA_ID);
        if (element == null) {
            return;
        }
        JsonObject data = new JsonObject();
        data.addProperty("stamina", Math.round(ClientStaminaState.stamina * 10.0F) / 10.0F);
        data.addProperty("max", Math.round(ClientStaminaState.max));
        String json = data.toString();
        if (!json.equals(lastData)) {
            lastData = json;
            element.setValue(json);
        }
    }

    private static void closeAui() {
        if (document != null) {
            document.remove();
            document = null;
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (mode != ClientConfig.StaminaHudMode.VANILLA) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int x = 10;
        int y = event.getWindow().getGuiScaledHeight() - 62;
        int width = 110;
        int height = 8;
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xB0000000);
        float ratio = ClientStaminaState.ratio();
        int color = ratio > 0.3F ? 0xFF46B96B : 0xFFD2703B;
        graphics.fill(x, y, x + Math.round(width * ratio), y + height, color);
        Component text = Component.translatable("hud.dndclasses.stamina")
                .append(" " + Math.round(ClientStaminaState.stamina) + " / " + Math.round(ClientStaminaState.max));
        graphics.drawString(minecraft.font, text, x, y - 11, 0xFFF0E2BE, true);
    }
}
