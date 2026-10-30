package mdh.dndclasses.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;

/**
 * Client-side alternation state for air swings (cosmetic only).
 * <p>
 * Entity attacks are decided by the server ({@code SwingHandPacket}); air swings never reach the
 * server, so the client keeps its own "next hand" marker on the local player to animate the offhand
 * on alternating air swings as well.
 */
public final class ClientDualWield {

    private static final String TAG_NEXT_OFF = "DndClientNextOff";

    private ClientDualWield() {
    }

    public static InteractionHand nextHand(LocalPlayer player) {
        return player.getPersistentData().getBoolean(TAG_NEXT_OFF)
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;
    }

    public static void flip(LocalPlayer player) {
        CompoundTag tag = player.getPersistentData();
        tag.putBoolean(TAG_NEXT_OFF, !tag.getBoolean(TAG_NEXT_OFF));
    }

    /** The server told us which hand it actually attacked with; the next swing is the other one. */
    public static void sync(LocalPlayer player, InteractionHand serverHand) {
        player.getPersistentData().putBoolean(TAG_NEXT_OFF, serverHand == InteractionHand.MAIN_HAND);
    }
}
