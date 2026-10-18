package mdh.dndclasses.network;

import mdh.dndclasses.client.SaveQteOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: open/refresh the save-QTE overlay with its (JSON) parameters. */
public class OpenSaveQtePacket {

    public final String qteId;
    public final String json;

    public OpenSaveQtePacket(String qteId, String json) {
        this.qteId = qteId;
        this.json = json == null ? "" : json;
    }

    public static void encode(OpenSaveQtePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.qteId, 64);
        buf.writeUtf(packet.json, 32767);
    }

    public static OpenSaveQtePacket decode(FriendlyByteBuf buf) {
        return new OpenSaveQtePacket(buf.readUtf(64), buf.readUtf(32767));
    }

    public static void handle(OpenSaveQtePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> () -> SaveQteOverlay.open(packet.qteId, packet.json)));
        context.setPacketHandled(true);
    }
}
