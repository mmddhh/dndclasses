package mdh.dndclasses.network;

import mdh.dndclasses.data.AsiSelection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -&gt; server: the player spent an Ability Score Improvement. */
public class AsiChoicePacket {

    private final String json;

    public AsiChoicePacket(String json) {
        this.json = json == null ? "{}" : json;
    }

    public String json() {
        return json;
    }

    public static void encode(AsiChoicePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.json, 4096);
    }

    public static AsiChoicePacket decode(FriendlyByteBuf buf) {
        return new AsiChoicePacket(buf.readUtf(4096));
    }

    public static void handle(AsiChoicePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player != null) {
                AsiSelection.applyJson(player, packet.json);
            }
        });
        context.setPacketHandled(true);
    }
}
