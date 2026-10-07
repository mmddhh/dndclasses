package mdh.dndclasses.network;

import mdh.dndclasses.client.ClientCharacterState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server -> client: character state as three capability NBT snapshots
 * (level / ability / spell) plus a few UI-only fields.
 */
public class ClientboundCharacterStatePacket {

    public final boolean created;
    public final boolean promptOpen;
    public final CompoundTag levelData;
    public final CompoundTag abilityData;
    public final CompoundTag spellData;
    public final int expLevelStart;
    public final int expToNext;
    public final List<String> featureIds;

    public ClientboundCharacterStatePacket(boolean created, boolean promptOpen, CompoundTag levelData,
                                            CompoundTag abilityData, CompoundTag spellData,
                                            int expLevelStart, int expToNext, List<String> featureIds) {
        this.created = created;
        this.promptOpen = promptOpen;
        this.levelData = levelData == null ? new CompoundTag() : levelData;
        this.abilityData = abilityData == null ? new CompoundTag() : abilityData;
        this.spellData = spellData == null ? new CompoundTag() : spellData;
        this.expLevelStart = expLevelStart;
        this.expToNext = expToNext;
        this.featureIds = featureIds == null ? List.of() : new ArrayList<>(featureIds);
    }

    public static void encode(ClientboundCharacterStatePacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.created);
        buf.writeBoolean(packet.promptOpen);
        buf.writeNbt(packet.levelData);
        buf.writeNbt(packet.abilityData);
        buf.writeNbt(packet.spellData);
        buf.writeVarInt(packet.expLevelStart);
        buf.writeVarInt(packet.expToNext);
        buf.writeVarInt(packet.featureIds.size());
        for (String id : packet.featureIds) {
            buf.writeUtf(id, 256);
        }
    }

    public static ClientboundCharacterStatePacket decode(FriendlyByteBuf buf) {
        boolean created = buf.readBoolean();
        boolean promptOpen = buf.readBoolean();
        CompoundTag levelData = buf.readNbt();
        CompoundTag abilityData = buf.readNbt();
        CompoundTag spellData = buf.readNbt();
        int expLevelStart = buf.readVarInt();
        int expToNext = buf.readVarInt();
        int size = buf.readVarInt();
        List<String> featureIds = new ArrayList<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            featureIds.add(buf.readUtf(256));
        }
        return new ClientboundCharacterStatePacket(created, promptOpen, levelData, abilityData, spellData,
                expLevelStart, expToNext, featureIds);
    }

    public static void handle(ClientboundCharacterStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCharacterState.apply(packet)));
        context.setPacketHandled(true);
    }
}
