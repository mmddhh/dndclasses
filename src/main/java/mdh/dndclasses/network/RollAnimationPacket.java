package mdh.dndclasses.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> tracking players: play a roll animation for the given entity. */
public class RollAnimationPacket {

    private final int entityId;
    private final int durationTicks;
    private final float yaw;
    private final boolean back;

    public RollAnimationPacket(int entityId, int durationTicks, float yaw, boolean back) {
        this.entityId = entityId;
        this.durationTicks = durationTicks;
        this.yaw = yaw;
        this.back = back;
    }

    public static void encode(RollAnimationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarInt(packet.durationTicks);
        buffer.writeFloat(packet.yaw);
        buffer.writeBoolean(packet.back);
    }

    public static RollAnimationPacket decode(FriendlyByteBuf buffer) {
        return new RollAnimationPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readFloat(), buffer.readBoolean());
    }

    public static void handle(RollAnimationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> mdh.dndclasses.client.RollAnimationState.start(
                packet.entityId, packet.durationTicks, packet.yaw, packet.back));
        context.setPacketHandled(true);
    }
}
