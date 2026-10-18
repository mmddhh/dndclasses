package mdh.dndclasses.network;

import mdh.dndclasses.feature.FeatureManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: try to use an active feature (radial wheel). */
public class UseFeaturePacket {

    private final ResourceLocation id;

    public UseFeaturePacket(ResourceLocation id) {
        this.id = id;
    }

    public static void encode(UseFeaturePacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.id);
    }

    public static UseFeaturePacket decode(FriendlyByteBuf buf) {
        return new UseFeaturePacket(buf.readResourceLocation());
    }

    public static void handle(UseFeaturePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || packet.id == null) {
                return;
            }
            FeatureManager.UseResult result = FeatureManager.use(player, packet.id);
            switch (result) {
                case SUCCESS -> player.displayClientMessage(Component.translatable(
                        "message.dndclasses.feature.used", FeatureManager.remainingUses(player, packet.id)), true);
                case UNKNOWN_FEATURE -> player.displayClientMessage(
                        Component.translatable("message.dndclasses.feature.unknown"), true);
                case NOT_ACTIVE -> player.displayClientMessage(
                        Component.translatable("message.dndclasses.feature.not_active"), true);
                case NO_USES -> player.displayClientMessage(
                        Component.translatable("message.dndclasses.feature.no_uses"), true);
            }
        });
        context.setPacketHandled(true);
    }
}
