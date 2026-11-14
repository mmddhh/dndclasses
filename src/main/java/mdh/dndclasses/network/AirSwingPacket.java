package mdh.dndclasses.network;

import mdh.dndclasses.weapon.DualWieldRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client tells the server which hand an air swing used. Air swings never fire
 * {@code AttackEntityEvent}, so without this the server's dual-wield alternation would not advance
 * and the next throw would use a stale hand.
 */
public record AirSwingPacket(InteractionHand hand) {

    public static void encode(AirSwingPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.hand);
    }

    public static AirSwingPacket decode(FriendlyByteBuf buf) {
        return new AirSwingPacket(buf.readEnum(InteractionHand.class));
    }

    public static void handle(AirSwingPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                DualWieldRules.recordAttack(player, packet.hand());
            }
        });
        context.setPacketHandled(true);
    }
}
