package mdh.dndclasses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * Client-side mirror of the server's reload timer.
 * <p>
 * The vanilla use-item state cannot drive the crossbow animation here: R starts the charge without
 * the player holding the use key, so Minecraft releases the use state again on the next tick and
 * the model would snap back after a single frame. Instead the crossbow's {@code pull}/{@code
 * pulling} model predicates read this timer, which lasts exactly as long as the server charge; the
 * arm is not posed as "using", but the crossbow itself shows the vanilla loading frames.
 */
public final class ClientReloadState {

    private static boolean active;
    private static long startTick;
    private static int durationTicks;

    private ClientReloadState() {
    }

    public static void start(int chargeTicks) {
        active = true;
        startTick = now();
        durationTicks = Math.max(1, chargeTicks);
    }

    public static void clear() {
        active = false;
        durationTicks = 0;
    }

    public static boolean isCharging() {
        return active && elapsed() < durationTicks;
    }

    /** 0.0 - 1.0 loading progress for the crossbow model. */
    public static float progress() {
        if (!isCharging()) {
            return 0.0F;
        }
        return Mth.clamp((float) elapsed() / (float) durationTicks, 0.0F, 1.0F);
    }

    public static void tick() {
        if (active && elapsed() >= durationTicks) {
            clear();
        }
    }

    private static long elapsed() {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, level.getGameTime() - startTick);
    }

    private static long now() {
        Level level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    }
}
