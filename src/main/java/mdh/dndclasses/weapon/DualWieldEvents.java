package mdh.dndclasses.weapon;

import mdh.dndclasses.Dndclasses;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dual-wield bookkeeping.
 * <p>
 * This class deliberately never moves items. Trying to "stow" an illegal offhand stack back into
 * the inventory is what caused duplicated ghost items (the slot write could be overwritten by the
 * caller while the copy had already been added). Instead the offhand rule is enforced by refusing
 * the benefit: an illegal offhand weapon never enables dual wielding and is never used for an
 * offhand attack, so the player has to move it out by hand.
 */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DualWieldEvents {

    private DualWieldEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            // Keep the dual-wield attack speed modifier in sync with the held weapons.
            DualWieldRules.updateAttackSpeed(player);
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide) {
            DualWieldRules.reset(player);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DualWieldRules.reset(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DualWieldRules.reset(event.getEntity());
    }
}
