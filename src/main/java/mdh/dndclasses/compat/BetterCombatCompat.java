package mdh.dndclasses.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Soft compatibility with Better Combat.
 * <p>
 * The mod never compiles against Better Combat: it only ships data files and reacts to Forge
 * events. This class is the single runtime detection point, so the whole compatibility layer can
 * be dropped when the systems are migrated elsewhere.
 * <p>
 * Better Combat's hand alternation is exposed through its public API
 * ({@code net.bettercombat.api.EntityPlayer_BetterCombat#getCurrentAttack}), which is queried
 * reflectively to keep the dependency optional.
 */
public final class BetterCombatCompat {

    private static Boolean loaded;
    private static Method getCurrentAttack;
    private static Method isOffHand;
    private static boolean reflectionUnavailable;

    private BetterCombatCompat() {
    }

    public static boolean isActive() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded("bettercombat");
        }
        return loaded;
    }

    /** True when Better Combat is currently resolving an offhand swing. */
    public static boolean isOffHandAttack(Player player) {
        if (!isActive() || reflectionUnavailable) {
            return false;
        }
        try {
            if (getCurrentAttack == null) {
                getCurrentAttack = player.getClass().getMethod("getCurrentAttack");
                isOffHand = getCurrentAttack.getReturnType().getMethod("isOffHand");
            }
            Object attack = getCurrentAttack.invoke(player);
            return attack != null && Boolean.TRUE.equals(isOffHand.invoke(attack));
        } catch (Throwable throwable) {
            reflectionUnavailable = true;
            return false;
        }
    }

    /** The stack whose D&amp;D rules should be used for the current swing. */
    public static ItemStack currentWeapon(Player player) {
        return isOffHandAttack(player) ? player.getOffhandItem() : player.getMainHandItem();
    }
}
