package mdh.dndclasses.weapon;

import mdh.dndclasses.Dndclasses;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Enforces the offhand equipment rule: an offhand weapon is allowed only when both the main hand
 * and the offhand hold <b>light</b> weapons (D&amp;D two-weapon fighting). Rejected items are
 * stowed back into the inventory instead of being deleted.
 * <p>
 * The rule is enforced on equipment changes and, as a safety net, periodically from the player
 * tick, because Forge's {@code LivingEquipmentChangeEvent} is not guaranteed to fire for every
 * player inventory path.
 */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DualWieldEvents {

    /** Guards against recursion when we move the rejected stack ourselves. */
    private static final ThreadLocal<Boolean> STOWING = ThreadLocal.withInitial(() -> false);

    private DualWieldEvents() {
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide || !player.isAlive() || STOWING.get()) {
            return;
        }
        enforce(player);
        DualWieldRules.reset(player);
    }

    /** Stows an offhand weapon that violates the light-weapon rule (server side only). */
    public static void enforce(ServerPlayer player) {
        if (STOWING.get() || !player.isAlive() || player.level().isClientSide) {
            return;
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof DndWeapon && !DualWieldRules.canHoldInOffHand(player, off)) {
            stow(player, off);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            DualWieldRules.reset(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DualWieldRules.reset(event.getEntity());
    }

    /** Moves a rejected offhand weapon back into the inventory (or drops it when full). */
    private static void stow(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        STOWING.set(true);
        try {
            ItemStack stowed = stack.copy();
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            if (!player.getOffhandItem().isEmpty()) {
                // The caller re-applied the stack after the event; retry on the next tick instead
                // of duplicating it now.
                return;
            }
            if (player.getMainHandItem().isEmpty()) {
                // Came from the main hand (offhand swap): put it straight back.
                player.setItemSlot(EquipmentSlot.MAINHAND, stowed);
            } else if (!player.getInventory().add(stowed)) {
                player.drop(stowed, false);
            }
            // Force the client to see the moved stack, otherwise the offhand keeps showing a ghost
            // item until the next full inventory sync (relog).
            player.inventoryMenu.broadcastChanges();
            player.displayClientMessage(
                    Component.translatable("message.dndclasses.offhand_light_only"), true);
        } finally {
            STOWING.set(false);
        }
    }
}
