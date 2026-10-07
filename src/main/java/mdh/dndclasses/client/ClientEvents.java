package mdh.dndclasses.client;

import mdh.dndclasses.Dndclasses;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Dndclasses.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {

    private static boolean pendingOpen = false;
    private static int pendingTicks = 0;

    private ClientEvents() {
    }

    public static void requestOpen() {
        pendingOpen = true;
        pendingTicks = 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        if (pendingOpen) {
            if (minecraft.screen == null) {
                pendingTicks++;
                if (pendingTicks > 40) {
                    pendingOpen = false;
                    minecraft.setScreen(new ClassCreationScreen());
                    return;
                }
            } else {
                pendingTicks = 0;
            }
        }

        while (ClientSetup.OPEN_CLASS_SCREEN.consumeClick()) {
            if (!ClientCharacterState.created) {
                minecraft.setScreen(new ClassCreationScreen());
            } else {
                minecraft.setScreen(new CharacterSheetScreen());
            }
        }
    }
}
