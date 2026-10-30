package mdh.dndclasses.network;

import mdh.dndclasses.client.SwingHandClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -&gt; client: play the attack swing on the given hand.
 * <p>
 * Vanilla's {@code LivingEntity#swing} ignores a new swing while the previous one is still in its
 * first half. The client predicts a main-hand swing as soon as the player clicks, so the server's
 * offhand swing one tick later is dropped and the offhand never animates. This packet clears the
 * local swing state before replaying it on the correct hand.
 */
public class SwingHandPacket {

    private final InteractionHand hand;

    public SwingHandPacket(InteractionHand hand) {
        this.hand = hand;
    }

    public InteractionHand hand() {
        return hand;
    }

    public static void encode(SwingHandPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.hand);
    }

    public static SwingHandPacket decode(FriendlyByteBuf buf) {
        return new SwingHandPacket(buf.readEnum(InteractionHand.class));
    }

    public static void handle(SwingHandPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SwingHandClientHandler.apply(packet)));
        context.setPacketHandled(true);
    }
}
