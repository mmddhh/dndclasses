package mdh.dndclasses.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: perform a dodge roll in the given world-space direction. */
public class RollPacket {

    private final float dirX;
    private final float dirZ;

    public RollPacket(float dirX, float dirZ) {
        this.dirX = dirX;
        this.dirZ = dirZ;
    }

    public static void encode(RollPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.dirX);
        buffer.writeFloat(packet.dirZ);
    }

    public static RollPacket decode(FriendlyByteBuf buffer) {
        return new RollPacket(buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(RollPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                mdh.dndclasses.stamina.StaminaManager.roll(context.getSender(), packet.dirX, packet.dirZ);
            }
        });
        context.setPacketHandled(true);
    }
}
