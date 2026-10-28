package mdh.dndclasses.stamina;

import mdh.dndclasses.armor.ArmorManager;
import mdh.dndclasses.weapon.AbilityMode;
import mdh.dndclasses.weapon.DndAttributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * D&amp;D stamina: 100 + CON modifier * 10. Sprinting drains, standing still regenerates. Rolling
 * costs stamina, scales with the worn armor and grants a short invulnerability window.
 */
public final class StaminaManager {

    public static final String TAG_STAMINA = "DndStamina";
    public static final String TAG_IFRAMES = "DndIFrames";
    public static final String TAG_COOLDOWN = "DndRollCooldown";

    public static final double SPRINT_DRAIN = 0.25D;
    public static final double REGEN = 0.5D;
    public static final double ROLL_COST = 20.0D;
    public static final int ROLL_COOLDOWN = 20;
    private static final int SYNC_INTERVAL = 5;

    private StaminaManager() {
    }

    public static int max(ServerPlayer player) {
        return Math.max(10, 100 + DndAttributes.modifier(player, AbilityMode.CONSTITUTION) * 10);
    }

    public static float get(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        float value = tag.contains(TAG_STAMINA) ? tag.getFloat(TAG_STAMINA) : max(player);
        return Mth.clamp(value, 0.0F, max(player));
    }

    public static void set(ServerPlayer player, float value) {
        player.getPersistentData().putFloat(TAG_STAMINA, Mth.clamp(value, 0.0F, max(player)));
    }

    public static boolean spend(ServerPlayer player, double amount) {
        float current = get(player);
        if (current + 1.0E-4D < amount) {
            return false;
        }
        set(player, current - (float) amount);
        return true;
    }

    public static void tick(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        int cooldown = tag.getInt(TAG_COOLDOWN);
        if (cooldown > 0) {
            tag.putInt(TAG_COOLDOWN, cooldown - 1);
        }
        int iframes = tag.getInt(TAG_IFRAMES);
        if (iframes > 0) {
            tag.putInt(TAG_IFRAMES, iframes - 1);
        }

        double multiplier = ArmorManager.getStaminaMultiplier(player);
        float current = get(player);
        if (player.isSprinting() && !player.isCreative() && !player.isSpectator()) {
            current -= (float) (SPRINT_DRAIN * multiplier);
            if (current <= 0.0F) {
                current = 0.0F;
                player.setSprinting(false);
            }
        } else {
            current += (float) REGEN;
        }
        set(player, current);

        if (player.tickCount % SYNC_INTERVAL == 0) {
            sync(player);
        }
    }

    public static void sync(ServerPlayer player) {
        mdh.dndclasses.network.DndNetwork.sendToPlayer(player,
                new mdh.dndclasses.network.StaminaSyncPacket(get(player), max(player)));
    }

    public static void roll(ServerPlayer player, float dirX, float dirZ) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        CompoundTag tag = player.getPersistentData();
        if (tag.getInt(TAG_COOLDOWN) > 0) {
            return;
        }
        double multiplier = ArmorManager.getStaminaMultiplier(player);
        if (!spend(player, ROLL_COST * multiplier)) {
            player.displayClientMessage(Component.translatable("message.dndclasses.no_stamina"), true);
            return;
        }

        float length = Mth.sqrt(dirX * dirX + dirZ * dirZ);
        if (length < 1.0E-4F) {
            Vec3 look = player.getLookAngle();
            dirX = (float) look.x;
            dirZ = (float) look.z;
            length = Mth.sqrt(dirX * dirX + dirZ * dirZ);
        }
        if (length < 1.0E-4F) {
            dirX = 0.0F;
            dirZ = 1.0F;
            length = 1.0F;
        }
        dirX /= length;
        dirZ /= length;

        Vec3 look = player.getLookAngle();
        boolean back = dirX * look.x + dirZ * look.z < -0.1D;
        float yaw = (float) (Math.atan2(-dirX, dirZ) * 180.0D / Math.PI);

        double distance = 0.9D * ArmorManager.getRollMultiplier(player);
        double vertical = player.onGround() ? 0.28D : 0.1D;
        player.setDeltaMovement(dirX * distance, vertical, dirZ * distance);
        player.hurtMarked = true;

        int iframes = 5 + ArmorManager.getAc(player) / 4;
        tag.putInt(TAG_IFRAMES, iframes);
        tag.putInt(TAG_COOLDOWN, ROLL_COOLDOWN);
        mdh.dndclasses.network.DndNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new mdh.dndclasses.network.RollAnimationPacket(player.getId(), iframes, yaw, back));
        sync(player);
    }

    public static boolean isInvulnerable(ServerPlayer player) {
        return player.getPersistentData().getInt(TAG_IFRAMES) > 0;
    }
}
