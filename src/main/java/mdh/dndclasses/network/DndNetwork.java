package mdh.dndclasses.network;

import mdh.dndclasses.Dndclasses;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class DndNetwork {

    private static final String PROTOCOL_VERSION = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Dndclasses.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private DndNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(ClassSelectionPacket.class, id++)
                .encoder(ClassSelectionPacket::encode)
                .decoder(ClassSelectionPacket::decode)
                .consumerMainThread(ClassSelectionPacket::handle)
                .add();

        CHANNEL.messageBuilder(ClientboundCharacterStatePacket.class, id++)
                .encoder(ClientboundCharacterStatePacket::encode)
                .decoder(ClientboundCharacterStatePacket::decode)
                .consumerMainThread(ClientboundCharacterStatePacket::handle)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, ClientboundCharacterStatePacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
