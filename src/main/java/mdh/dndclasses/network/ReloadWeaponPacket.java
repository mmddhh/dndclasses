package mdh.dndclasses.network;

import mdh.dndclasses.weapon.DndCombat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: reload the held LOADING weapon (crossbows / blowgun). */
public class ReloadWeaponPacket {

    public ReloadWeaponPacket() {
    }

    public static void encode(ReloadWeaponPacket packet, FriendlyByteBuf buffer) {
    }

    public static ReloadWeaponPacket decode(FriendlyByteBuf buffer) {
        return new ReloadWeaponPacket();
    }

    public static void handle(ReloadWeaponPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                DndCombat.startReload(player);
            }
        });
        context.setPacketHandled(true);
    }
}
