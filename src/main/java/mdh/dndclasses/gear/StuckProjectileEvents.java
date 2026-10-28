package mdh.dndclasses.gear;

import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.capability.ModCapabilities;
import mdh.dndclasses.capability.Provider;
import mdh.dndclasses.network.DndNetwork;
import mdh.dndclasses.network.StuckProjectileSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Server-side lifecycle of stuck projectiles: capability attachment, client sync, death drops.
 * <p>
 * Retrieval is death-drop only (no right-click interaction), so nothing here touches vanilla
 * entity interaction.
 */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StuckProjectileEvents {

    private static final ResourceLocation CAPABILITY_ID =
            new ResourceLocation(Dndclasses.MODID, "stuck_projectiles");

    private StuckProjectileEvents() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof LivingEntity)) {
            return;
        }
        Provider<IStuckProjectiles> provider =
                new Provider<>(ModCapabilities.STUCK_PROJECTILES_CAPABILITY, new StuckProjectiles());
        event.addCapability(CAPABILITY_ID, provider);
        event.addListener(provider::invalidate);
    }

    /** A player that starts tracking an entity needs the full snapshot (dimension change, relog, join). */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        IStuckProjectiles capability = StuckProjectiles.get(target);
        if (capability == null) {
            return;
        }
        DndNetwork.sendToPlayer(player,
                new StuckProjectileSyncPacket(target.getId(), capability.getAll()));
    }

    /** Dying drops every projectile stuck in the body, then clears the list so nothing drops twice. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        IStuckProjectiles capability = StuckProjectiles.get(entity);
        if (capability == null || capability.size() == 0) {
            return;
        }
        for (StuckProjectile projectile : capability.getAll()) {
            if (projectile.stack().isEmpty()) {
                continue;
            }
            ItemEntity drop = new ItemEntity(entity.level(),
                    entity.getX(),
                    entity.getY() + entity.getBbHeight() * 0.5D,
                    entity.getZ(),
                    projectile.stack().copy());
            drop.setDefaultPickUpDelay();
            event.getDrops().add(drop);
        }
        capability.clear();
    }

    /** Broadcasts the current list to every player tracking the entity (plus the entity itself if it is a player). */
    public static void sync(LivingEntity target) {
        IStuckProjectiles capability = StuckProjectiles.get(target);
        if (capability == null) {
            return;
        }
        StuckProjectileSyncPacket packet =
                new StuckProjectileSyncPacket(target.getId(), capability.getAll());
        DndNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> target), packet);
        if (target instanceof ServerPlayer self) {
            DndNetwork.sendToPlayer(self, packet);
        }
    }
}
