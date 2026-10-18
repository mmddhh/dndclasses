package mdh.dndclasses.conditions;

import mdh.dndclasses.Dndclasses;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Enforces condition flags that need real-time event interception. */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ConditionEvents {

    private ConditionEvents() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (ConditionManager.isIncapacitated(player)) {
            deny(player);
            event.setCanceled(true);
            return;
        }
        Entity target = event.getTarget();
        if (target != null && ConditionManager.cannotAttackSource(player, target.getUUID())) {
            player.displayClientMessage(Component.translatable("message.dndclasses.condition.cannot_attack"), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && ConditionManager.isIncapacitated(player)) {
            deny(player);
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && ConditionManager.isIncapacitated(player)) {
            deny(player);
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && ConditionManager.isIncapacitated(player)) {
            deny(player);
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof ServerPlayer player && ConditionManager.isIncapacitated(player)) {
            deny(player);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0 && ConditionManager.halfDamage(player)) {
            event.setAmount(event.getAmount() * 0.5F);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) {
            return;
        }
        Player player = event.player;
        if (player instanceof ServerPlayer serverPlayer && ConditionManager.isImmobile(serverPlayer)) {
            player.setDeltaMovement(0.0D, player.getDeltaMovement().y, 0.0D);
            player.setSprinting(false);
        }
    }

    private static void deny(Player player) {
        player.displayClientMessage(Component.translatable("message.dndclasses.condition.incapacitated"), true);
    }
}
