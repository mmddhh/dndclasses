package mdh.dndclasses.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: current stamina values for the HUD. */
public class StaminaSyncPacket {

    private final float stamina;
    private final float max;

    public StaminaSyncPacket(float stamina, float max) {
        this.stamina = stamina;
        this.max = max;
    }

    public static void encode(StaminaSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.stamina);
        buffer.writeFloat(packet.max);
    }

    public static StaminaSyncPacket decode(FriendlyByteBuf buffer) {
        return new StaminaSyncPacket(buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(StaminaSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> mdh.dndclasses.client.ClientStaminaState.set(packet.stamina, packet.max));
        context.setPacketHandled(true);
    }
}
