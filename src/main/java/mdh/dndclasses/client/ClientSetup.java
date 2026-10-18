package mdh.dndclasses.client;

import com.mojang.blaze3d.platform.InputConstants;
import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.weapon.WeaponEntities;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
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

    public static final KeyMapping OPEN_FEATURE_WHEEL = new KeyMapping(
            "key.dndclasses.open_feature_wheel",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KEY_CATEGORY
    );

    public static final KeyMapping SAVE_QTE_KEY = new KeyMapping(
            "key.dndclasses.save_qte",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            KEY_CATEGORY
    );

    public static final KeyMapping RELOAD_WEAPON = new KeyMapping(
            "key.dndclasses.reload_weapon",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KEY_CATEGORY
    );

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CLASS_SCREEN);
        event.register(OPEN_FEATURE_WHEEL);
        event.register(SAVE_QTE_KEY);
        event.register(RELOAD_WEAPON);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WeaponEntities.THROWN_WEAPON.get(), ThrownItemRenderer::new);
    }
}
