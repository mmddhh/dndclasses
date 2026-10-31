package mdh.dndclasses.network;

import mdh.dndclasses.client.ChoiceScreenOpener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -&gt; client: open a selection page ("asi", "subclass") or close it ("close"). */
public class OpenChoicePacket {

    private final String json;

    public OpenChoicePacket(String json) {
        this.json = json == null ? "{}" : json;
    }

    public String json() {
        return json;
    }

    public static void encode(OpenChoicePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.json, 32767);
    }

    public static OpenChoicePacket decode(FriendlyByteBuf buf) {
        return new OpenChoicePacket(buf.readUtf(32767));
    }

    public static void handle(OpenChoicePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ChoiceScreenOpener.open(packet)));
        context.setPacketHandled(true);
    }
}
