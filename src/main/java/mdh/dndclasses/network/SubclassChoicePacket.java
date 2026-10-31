package mdh.dndclasses.network;

import mdh.dndclasses.data.SubclassSelection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -&gt; server: the player picked a subclass in the selection page. */
public class SubclassChoicePacket {

    private final String id;

    public SubclassChoicePacket(String id) {
        this.id = id == null ? "" : id;
    }

    public String id() {
        return id;
    }

    public static void encode(SubclassChoicePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.id, 256);
    }

    public static SubclassChoicePacket decode(FriendlyByteBuf buf) {
        return new SubclassChoicePacket(buf.readUtf(256));
    }

    public static void handle(SubclassChoicePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player != null) {
                SubclassSelection.pick(player, packet.id);
            }
        });
        context.setPacketHandled(true);
    }
}
