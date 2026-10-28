package mdh.dndclasses.client;

import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.gear.StuckProjectile;
import mdh.dndclasses.network.StuckProjectileSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies {@link StuckProjectileSyncPacket} snapshots to the client-side capability.
 * <p>
 * A snapshot can arrive before the tracked entity exists on the client (spawn packet ordering), so
 * unresolved entity ids are parked here and applied on {@code EntityJoinLevelEvent}.
 */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StuckProjectileClientHandler {

    private static final Map<Integer, List<StuckProjectile>> PENDING = new HashMap<>();

    private StuckProjectileClientHandler() {
    }

    public static void apply(StuckProjectileSyncPacket packet) {
        Entity entity = find(packet.entityId());
        if (entity == null) {
            PENDING.put(packet.entityId(), packet.projectiles());
            return;
        }
        PENDING.remove(packet.entityId());
        store(entity, packet.projectiles());
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (PENDING.isEmpty()) {
            return;
        }
        List<StuckProjectile> pending = PENDING.remove(event.getEntity().getId());
        if (pending != null) {
            store(event.getEntity(), pending);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        PENDING.clear();
    }

    private static Entity find(int entityId) {
        Level level = Minecraft.getInstance().level;
        return level == null ? null : level.getEntity(entityId);
    }

    private static void store(Entity entity, List<StuckProjectile> projectiles) {
        entity.getCapability(ModCapabilities.STUCK_PROJECTILES_CAPABILITY)
                .ifPresent(capability -> capability.setAll(projectiles));
    }
}
