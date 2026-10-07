package mdh.dndclasses.client;

import com.mojang.blaze3d.platform.InputConstants;
import mdh.dndclasses.Dndclasses;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Dndclasses.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

    public static final String KEY_CATEGORY = "key.categories.dndclasses";

    public static final KeyMapping OPEN_CLASS_SCREEN = new KeyMapping(
            "key.dndclasses.open_class_screen",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            KEY_CATEGORY
    );

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CLASS_SCREEN);
    }
}
