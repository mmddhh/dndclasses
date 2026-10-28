package mdh.dndclasses.weapon;

import mdh.dndclasses.Dndclasses;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Enforces the offhand equipment rule: an offhand weapon requires a light main-hand weapon
 * (D&amp;D two-weapon fighting). Rejected items are stowed back into the inventory instead of being
 * deleted. Also resets the attack alternation when equipment changes.
 */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DualWieldEvents {

    /** Guards against recursion when we move the rejected stack ourselves. */
    private static final ThreadLocal<Boolean> STOWING = ThreadLocal.withInitial(() -> false);

    private DualWieldEvents() {
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        if (!player.isAlive() || player.isDeadOrDying() || STOWING.get()) {
            return;
        }

        if (event.getSlot() == EquipmentSlot.OFFHAND) {
            ItemStack to = event.getTo();
            if (to.getItem() instanceof DndWeapon && !DualWieldRules.canHoldInOffHand(player, to)) {
                stow(player, EquipmentSlot.OFFHAND, to);
            }
        } else if (event.getSlot() == EquipmentSlot.MAINHAND) {
            ItemStack main = event.getTo();
            ItemStack off = player.getOffhandItem();
            boolean mainAllowsOffhand = !(main.getItem() instanceof DndWeapon mainWeapon)
                    || mainWeapon.spec().has(WeaponProperty.LIGHT);
            if (!mainAllowsOffhand && off.getItem() instanceof DndWeapon) {
                stow(player, EquipmentSlot.OFFHAND, off);
            }
        }

        DualWieldRules.reset(player);
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
    private static void stow(Player player, EquipmentSlot slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        STOWING.set(true);
        try {
            ItemStack stowed = stack.copy();
            player.setItemSlot(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(stowed)) {
                player.drop(stowed, false);
            }
            player.displayClientMessage(
                    Component.translatable("message.dndclasses.offhand_light_only"), true);
        } finally {
            STOWING.set(false);
        }
    }
}
