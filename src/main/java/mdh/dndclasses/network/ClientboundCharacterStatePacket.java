package mdh.dndclasses.network;

import mdh.dndclasses.client.ClientCharacterState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server -> client: character state as three capability NBT snapshots
 * (level / ability / spell) plus a few UI-only fields, including the player's
 * active features for the radial wheel.
 */
public class ClientboundCharacterStatePacket {

    /** One radial-wheel entry: an active feature with its display/localization data and remaining uses. */
    public record FeatureEntry(ResourceLocation id, String nameKey, int remaining, int max, String rest) {
    }

    public final boolean created;
    public final boolean promptOpen;
    public final CompoundTag levelData;
    public final CompoundTag abilityData;
    public final CompoundTag spellData;
    public final CompoundTag conditionsData;
    public final int expLevelStart;
    public final int expToNext;
    public final List<FeatureEntry> features;

    public ClientboundCharacterStatePacket(boolean created, boolean promptOpen, CompoundTag levelData,
                                           CompoundTag abilityData, CompoundTag spellData, CompoundTag conditionsData,
                                           int expLevelStart, int expToNext, List<FeatureEntry> features) {
        this.created = created;
        this.promptOpen = promptOpen;
        this.levelData = levelData == null ? new CompoundTag() : levelData;
        this.abilityData = abilityData == null ? new CompoundTag() : abilityData;
        this.spellData = spellData == null ? new CompoundTag() : spellData;
        this.conditionsData = conditionsData == null ? new CompoundTag() : conditionsData;
        this.expLevelStart = expLevelStart;
        this.expToNext = expToNext;
        this.features = features == null ? List.of() : new ArrayList<>(features);
    }

    public static void encode(ClientboundCharacterStatePacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.created);
        buf.writeBoolean(packet.promptOpen);
        buf.writeNbt(packet.levelData);
        buf.writeNbt(packet.abilityData);
        buf.writeNbt(packet.spellData);
        buf.writeNbt(packet.conditionsData);
        buf.writeVarInt(packet.expLevelStart);
        buf.writeVarInt(packet.expToNext);
        buf.writeVarInt(packet.features.size());
        for (FeatureEntry entry : packet.features) {
            buf.writeResourceLocation(entry.id());
            buf.writeUtf(entry.nameKey(), 256);
            buf.writeVarInt(entry.remaining());
            buf.writeVarInt(entry.max());
            buf.writeUtf(entry.rest(), 16);
        }
    }

    public static ClientboundCharacterStatePacket decode(FriendlyByteBuf buf) {
        boolean created = buf.readBoolean();
        boolean promptOpen = buf.readBoolean();
        CompoundTag levelData = buf.readNbt();
        CompoundTag abilityData = buf.readNbt();
        CompoundTag spellData = buf.readNbt();
        CompoundTag conditionsData = buf.readNbt();
        int expLevelStart = buf.readVarInt();
        int expToNext = buf.readVarInt();
        int size = buf.readVarInt();
        List<FeatureEntry> features = new ArrayList<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buf.readResourceLocation();
            String nameKey = buf.readUtf(256);
            int remaining = buf.readVarInt();
            int max = buf.readVarInt();
            String rest = buf.readUtf(16);
            features.add(new FeatureEntry(id, nameKey, remaining, max, rest));
        }
        return new ClientboundCharacterStatePacket(created, promptOpen, levelData, abilityData, spellData,
                conditionsData, expLevelStart, expToNext, features);
    }

    public static void handle(ClientboundCharacterStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCharacterState.apply(packet)));
        context.setPacketHandled(true);
    }
}
