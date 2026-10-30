package mdh.dndclasses.client;

import mdh.dndclasses.network.SwingHandPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Forces the first-person swing of the hand chosen by the server. */
public final class SwingHandClientHandler {

    private SwingHandClientHandler() {
    }

    public static void apply(SwingHandPacket packet) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        // Clear the predicted swing so LivingEntity#swing accepts the new one, then replay it on
        // the hand the server actually attacked with.
        player.swinging = false;
        player.swing(packet.hand(), false);
        ClientDualWield.sync(player, packet.hand());
    }
}
