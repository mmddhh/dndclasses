package mdh.dndclasses.network;

import mdh.dndclasses.client.StuckProjectileClientHandler;
import mdh.dndclasses.gear.StuckProjectile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server -> client: full list of projectiles stuck in one entity.
 * <p>
 * Always a full snapshot instead of add/remove deltas: the list is capped at 64 small entries and
 * one packet type means one code path for "entity starts being tracked", "projectile attached" and
 * "projectile removed".
 */
public class StuckProjectileSyncPacket {

    private final int entityId;
    private final List<StuckProjectile> projectiles;

    public StuckProjectileSyncPacket(int entityId, List<StuckProjectile> projectiles) {
        this.entityId = entityId;
        this.projectiles = projectiles == null ? List.of() : new ArrayList<>(projectiles);
    }

    public int entityId() {
        return entityId;
    }

    public List<StuckProjectile> projectiles() {
        return projectiles;
    }

    public static void encode(StuckProjectileSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeVarInt(packet.projectiles.size());
        for (StuckProjectile projectile : packet.projectiles) {
            projectile.write(buf);
        }
    }

    public static StuckProjectileSyncPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int size = Math.min(buf.readVarInt(), StuckProjectile.MAX_PER_ENTITY);
        List<StuckProjectile> projectiles = new ArrayList<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            projectiles.add(StuckProjectile.read(buf));
        }
        return new StuckProjectileSyncPacket(entityId, projectiles);
    }

    public static void handle(StuckProjectileSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> StuckProjectileClientHandler.apply(packet)));
        context.setPacketHandled(true);
    }
}
