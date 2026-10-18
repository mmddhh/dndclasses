package mdh.dndclasses.network;

import mdh.dndclasses.saves.SaveQteManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: one slot of a save-QTE was judged (sent the moment a block is resolved). */
public class SaveQteResultPacket {

    public final String qteId;
    public final int index;
    public final boolean success;

    public SaveQteResultPacket(String qteId, int index, boolean success) {
        this.qteId = qteId == null ? "" : qteId;
        this.index = index;
        this.success = success;
    }

    public static void encode(SaveQteResultPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.qteId, 64);
        buf.writeVarInt(packet.index);
        buf.writeBoolean(packet.success);
    }

    public static SaveQteResultPacket decode(FriendlyByteBuf buf) {
        return new SaveQteResultPacket(buf.readUtf(64), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(SaveQteResultPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                SaveQteManager.resolveSlot(player, packet.qteId, packet.index, packet.success);
            }
        });
        context.setPacketHandled(true);
    }
}
