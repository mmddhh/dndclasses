package mdh.dndclasses.client;

import com.mojang.blaze3d.platform.InputConstants;
import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.weapon.DndWeaponItem;
import mdh.dndclasses.weapon.DndWeaponSpec;
import mdh.dndclasses.weapon.WeaponEntities;
import mdh.dndclasses.weapon.WeaponItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;
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

    public static final KeyMapping ROLL = new KeyMapping(
            "key.dndclasses.roll",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_4,
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
        event.register(ROLL);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WeaponEntities.THROWN_WEAPON.get(), DndThrownWeaponRenderer::new);
        event.registerEntityRenderer(WeaponEntities.DND_ARROW.get(), DndArrowRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (RegistryObject<? extends Item> registryObject : WeaponItems.WEAPONS.values()) {
                Item item = registryObject.get();
                DndWeaponSpec spec = WeaponItems.specOf(item);
                if (spec == null) {
                    continue;
                }
                if (spec.isCrossbow()) {
                    // Loading animation is driven by our own charge timer, not the use-item state
                    // (R does not hold the use key, so vanilla would release it on the next tick).
                    // The per-stack tag keeps every other crossbow in the hotbar idle.
                    ItemProperties.register(item, new ResourceLocation("pull"), (stack, level, entity, seed) ->
                            isReloading(stack) ? ClientReloadState.progress() : 0.0F);
                    ItemProperties.register(item, new ResourceLocation("pulling"), (stack, level, entity, seed) ->
                            isReloading(stack) && ClientReloadState.isCharging() ? 1.0F : 0.0F);
                    ItemProperties.register(item, new ResourceLocation("charged"), (stack, level, entity, seed) ->
                            stack.getOrCreateTag().getBoolean(DndWeaponItem.TAG_LOADED) ? 1.0F : 0.0F);
                } else if (spec.isBow()) {
                    ItemProperties.register(item, new ResourceLocation("pull"), (stack, level, entity, seed) -> {
                        if (entity == null || !entity.isUsingItem() || entity.getUseItem() != stack) {
                            return 0.0F;
                        }
                        return (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 20.0F;
                    });
                    ItemProperties.register(item, new ResourceLocation("pulling"), (stack, level, entity, seed) ->
                            entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
                }
            }
        });
    }

    /** True only for the exact crossbow stack that is currently being reloaded. */
    private static boolean isReloading(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(DndWeaponItem.TAG_RELOADING);
    }
}
