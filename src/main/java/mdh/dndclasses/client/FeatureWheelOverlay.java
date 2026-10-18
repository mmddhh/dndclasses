package mdh.dndclasses.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sighs.apricityui.ApricityUI;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.layout.Position;
import com.sighs.apricityui.layout.Size;
import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.network.ClientboundCharacterStatePacket.FeatureEntry;
import mdh.dndclasses.network.DndNetwork;
import mdh.dndclasses.network.UseFeaturePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Radial "feature wheel" rendered as an ApricityUI Overlay (a Document not attached to any Screen),
 * so player movement and hotbar switching keep working while it is open.
 *
 * <p>Hover AND click are both handled here in Java:
 * {@code getMouseDocumentPosition()} gives reliable document coordinates for hover (pushed to
 * {@code #hover-index}), and a left click is taken straight from {@code InputEvent.MouseButton.Pre}
 * (which fires even with no Screen / released cursor) and cancelled so the game never re-grabs the
 * cursor or treats it as a world click. The page is pure presentation + {@code #feature-data}.
 */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FeatureWheelOverlay {

    private static final String PAGE = "screens/feature_wheel.html";
    private static final String DATA_ID = "feature-data";
    private static final String HOVER_ID = "hover-index";

    private static final double R_IN = 74.0;
    private static final double R_OUT = 208.0;

    private static final int LEFT_BUTTON = 0;
    private static final int PRESS = 1;

    private static Document document;
    private static boolean open = false;
    private static String lastData = "";
    private static int lastHover = -999;

    private FeatureWheelOverlay() {
    }

    public static boolean isOpen() {
        return open;
    }

    public static void toggle() {
        if (open) {
            close(true);
        } else {
            open();
        }
    }

    public static void open() {
        if (open) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !ClientCharacterState.created) {
            return;
        }
        Document created = ApricityUI.createDocument(PAGE);
        if (created == null) {
            return;
        }
        document = created;
        open = true;
        lastData = "";
        lastHover = -999;
        minecraft.mouseHandler.releaseMouse();
    }

    /** @param grabMouse re-grab the cursor (false on logout / level unload, where there is no world). */
    public static void close(boolean grabMouse) {
        if (!open && document == null) {
            return;
        }
        if (document != null) {
            document.remove();
            document = null;
        }
        open = false;
        if (grabMouse) {
            Minecraft.getInstance().mouseHandler.grabMouse();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !open) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        // Any real Screen takes over, or the document vanished (resource reload): close.
        if (minecraft.player == null || minecraft.screen != null) {
            close(false);
            return;
        }
        pushData();
        pushHover();
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (!open || event.getAction() != PRESS || event.getButton() != LEFT_BUTTON) {
            return;
        }
        // Cancel so the game does not re-grab the cursor (MouseHandler.onPress) or use the world.
        event.setCanceled(true);
        int index = currentHoverIndex();
        if (index >= 0 && index < ClientCharacterState.features.size()
                && Minecraft.getInstance().getConnection() != null) {
            DndNetwork.CHANNEL.sendToServer(new UseFeaturePacket(ClientCharacterState.features.get(index).id()));
        }
        close(true);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        close(false);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            close(false);
        }
    }

    private static void pushData() {
        Element dataElement = document.getElementById(DATA_ID);
        if (dataElement == null) {
            return;
        }
        String json = buildData();
        if (!json.equals(lastData)) {
            lastData = json;
            dataElement.setValue(json);
        }
    }

    private static String buildData() {
        JsonObject root = new JsonObject();
        root.addProperty("class", ClientCharacterState.classKey);

        JsonArray features = new JsonArray();
        for (FeatureEntry entry : ClientCharacterState.features) {
            JsonObject f = new JsonObject();
            f.addProperty("id", entry.id().toString());
            f.addProperty("name", Component.translatable(entry.nameKey()).getString());
            f.addProperty("remaining", entry.remaining());
            f.addProperty("max", entry.max());
            f.addProperty("rest", entry.rest());
            features.add(f);
        }
        root.add("features", features);

        JsonObject strings = new JsonObject();
        strings.addProperty("title", tr("screen.dndclasses.wheel.title"));
        strings.addProperty("noFeatures", tr("screen.dndclasses.wheel.no_features"));
        strings.addProperty("uses", tr("screen.dndclasses.wheel.uses"));
        strings.addProperty("shortRest", tr("screen.dndclasses.wheel.rest_short"));
        strings.addProperty("longRest", tr("screen.dndclasses.wheel.rest_long"));
        strings.addProperty("hint", tr("screen.dndclasses.wheel.hint"));
        root.add("strings", strings);

        return root.toString();
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    private static void pushHover() {
        Element hoverElement = document.getElementById(HOVER_ID);
        if (hoverElement == null) {
            return;
        }
        int hoverIndex = currentHoverIndex();
        if (hoverIndex != lastHover) {
            lastHover = hoverIndex;
            hoverElement.setValue(Integer.toString(hoverIndex));
        }
    }

    /** Sector index under the cursor right now, or -1. */
    private static int currentHoverIndex() {
        if (document == null) {
            return -1;
        }
        Position mouse = document.getMouseDocumentPosition();
        Size viewport = document.getViewportSize();
        if (mouse == null || viewport == null) {
            return -1;
        }
        double dx = mouse.x - viewport.width() / 2.0;
        double dy = mouse.y - viewport.height() / 2.0;
        return hoverIndex(dx, dy);
    }

    private static int hoverIndex(double dx, double dy) {
        int n = ClientCharacterState.features.size();
        if (n == 0) {
            return -1;
        }
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < R_IN || dist > R_OUT) {
            return -1;
        }
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
        if (angle < 0) {
            angle += 360.0;
        }
        if (angle >= 360.0) {
            angle -= 360.0;
        }
        double step = 360.0 / n;
        return (int) Math.floor(angle / step) % n;
    }
}
