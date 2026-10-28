package mdh.dndclasses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/** Client-side roll animation timers, keyed by entity id. */
public final class RollAnimationState {

    public record Animation(long startGameTime, int durationTicks, float yaw, boolean back) {
    }

    private static final Map<Integer, Animation> ACTIVE = new HashMap<>();

    private RollAnimationState() {
    }

    public static void start(int entityId, int durationTicks, float yaw, boolean back) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || durationTicks <= 0) {
            return;
        }
        ACTIVE.put(entityId, new Animation(minecraft.level.getGameTime(), durationTicks, yaw, back));
    }

    public static Animation get(int entityId) {
        return ACTIVE.get(entityId);
    }

    public static float progress(Animation animation, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 1.0F;
        }
        float elapsed = (minecraft.level.getGameTime() - animation.startGameTime()) + partialTick;
        return Mth.clamp(elapsed / animation.durationTicks(), 0.0F, 1.0F);
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            ACTIVE.clear();
            return;
        }
        long now = minecraft.level.getGameTime();
        ACTIVE.entrySet().removeIf(entry -> now - entry.getValue().startGameTime() > entry.getValue().durationTicks());
    }
}
