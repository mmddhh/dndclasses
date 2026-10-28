package mdh.dndclasses.weapon;

import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.armor.ArmorManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Monster critical hits. Player attacks are resolved in {@link DndCombat}; here any non-player
 * living attacker has a 5% chance to crit a player for 1.5x damage, unless an orichalcum set makes
 * the wearer crit-immune.
 */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CritEvents {

    private CritEvents() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity) || source instanceof Player) {
            return;
        }
        if (ArmorManager.isCritImmune(player) || player.getRandom().nextFloat() >= 0.05F) {
            return;
        }
        event.setAmount(event.getAmount() * 1.5F);
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(),
                    12, 0.3D, 0.3D, 0.3D, 0.1D);
        }
    }
}
