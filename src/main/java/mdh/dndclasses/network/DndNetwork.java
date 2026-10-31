package mdh.dndclasses.network;

import mdh.dndclasses.Dndclasses;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class DndNetwork {

    private static final String PROTOCOL_VERSION = "11";

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

        CHANNEL.messageBuilder(UseFeaturePacket.class, id++)
                .encoder(UseFeaturePacket::encode)
                .decoder(UseFeaturePacket::decode)
                .consumerMainThread(UseFeaturePacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenSaveQtePacket.class, id++)
                .encoder(OpenSaveQtePacket::encode)
                .decoder(OpenSaveQtePacket::decode)
                .consumerMainThread(OpenSaveQtePacket::handle)
                .add();

        CHANNEL.messageBuilder(SaveQteResultPacket.class, id++)
                .encoder(SaveQteResultPacket::encode)
                .decoder(SaveQteResultPacket::decode)
                .consumerMainThread(SaveQteResultPacket::handle)
                .add();

        CHANNEL.messageBuilder(ReloadWeaponPacket.class, id++)
                .encoder(ReloadWeaponPacket::encode)
                .decoder(ReloadWeaponPacket::decode)
                .consumerMainThread(ReloadWeaponPacket::handle)
                .add();

        CHANNEL.messageBuilder(RollPacket.class, id++)
                .encoder(RollPacket::encode)
                .decoder(RollPacket::decode)
                .consumerMainThread(RollPacket::handle)
                .add();

        CHANNEL.messageBuilder(StaminaSyncPacket.class, id++)
                .encoder(StaminaSyncPacket::encode)
                .decoder(StaminaSyncPacket::decode)
                .consumerMainThread(StaminaSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(RollAnimationPacket.class, id++)
                .encoder(RollAnimationPacket::encode)
                .decoder(RollAnimationPacket::decode)
                .consumerMainThread(RollAnimationPacket::handle)
                .add();

        CHANNEL.messageBuilder(StuckProjectileSyncPacket.class, id++)
                .encoder(StuckProjectileSyncPacket::encode)
                .decoder(StuckProjectileSyncPacket::decode)
                .consumerMainThread(StuckProjectileSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(SwingHandPacket.class, id++)
                .encoder(SwingHandPacket::encode)
                .decoder(SwingHandPacket::decode)
                .consumerMainThread(SwingHandPacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenChoicePacket.class, id++)
                .encoder(OpenChoicePacket::encode)
                .decoder(OpenChoicePacket::decode)
                .consumerMainThread(OpenChoicePacket::handle)
                .add();

        CHANNEL.messageBuilder(SubclassChoicePacket.class, id++)
                .encoder(SubclassChoicePacket::encode)
                .decoder(SubclassChoicePacket::decode)
                .consumerMainThread(SubclassChoicePacket::handle)
                .add();

        CHANNEL.messageBuilder(AsiChoicePacket.class, id++)
                .encoder(AsiChoicePacket::encode)
                .decoder(AsiChoicePacket::decode)
                .consumerMainThread(AsiChoicePacket::handle)
                .add();
    }

    public static <MSG> void sendToPlayer(ServerPlayer player, MSG packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
