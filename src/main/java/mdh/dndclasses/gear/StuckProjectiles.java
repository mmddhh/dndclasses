package mdh.dndclasses.gear;

import mdh.dndclasses.capability.ModCapabilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Default {@link IStuckProjectiles} implementation: a small ordered list persisted in the entity's
 * capability NBT ({@code ForgeCaps -> dndclasses:stuck_projectiles -> Projectiles[...]}).
 */
public final class StuckProjectiles implements IStuckProjectiles, INBTSerializable<CompoundTag> {

    private static final String TAG_PROJECTILES = "Projectiles";

    private final List<StuckProjectile> projectiles = new ArrayList<>();

    /** Capability lookup; {@code null} when the entity has no provider (should not happen for alive entities). */
    public static IStuckProjectiles get(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        return entity.getCapability(ModCapabilities.STUCK_PROJECTILES_CAPABILITY).orElse(null);
    }

    /** Read-only view used by the renderer; empty when the entity has no capability. */
    public static List<StuckProjectile> listOf(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return List.of();
        }
        IStuckProjectiles capability = get(living);
        return capability == null ? List.of() : capability.getAll();
    }

    /**
     * Server side: convert a world-space hit into body space and store the projectile.
     *
     * @param target living entity that was hit
     * @param stack  item to render (and to drop later); copied
     * @param hit    exact impact point, as reported by {@code EntityHitResult#getLocation()}
     * @param motion projectile velocity at the moment of the hit
     * @param kind   which mesh the client should draw
     * @return true when the projectile was stored
     */
    public static boolean attach(LivingEntity target, ItemStack stack, Vec3 hit, Vec3 motion,
                                 StuckProjectile.Kind kind) {
        if (target == null || stack == null || stack.isEmpty()) {
            return false;
        }
        IStuckProjectiles capability = get(target);
        if (capability == null) {
            return false;
        }

        float bodyYaw = target.yBodyRot;
        Vec3 offset = StuckProjectile.worldToBody(hit.subtract(target.position()), bodyYaw);
        Vec3 dir = motion.lengthSqr() > 1.0E-8D
                ? StuckProjectile.worldToBody(motion.normalize(), bodyYaw)
                : new Vec3(0.0D, -1.0D, 0.0D);

        capability.add(new StuckProjectile(nextId(capability), stack.copy(), offset, dir,
                kind == null ? StuckProjectile.Kind.ITEM : kind));
        return true;
    }

    private static int nextId(IStuckProjectiles capability) {
        int highest = 0;
        for (StuckProjectile projectile : capability.getAll()) {
            highest = Math.max(highest, projectile.id());
        }
        return highest + 1;
    }

    @Override
    public List<StuckProjectile> getAll() {
        return Collections.unmodifiableList(projectiles);
    }

    @Override
    public void setAll(List<StuckProjectile> incoming) {
        projectiles.clear();
        if (incoming == null) {
            return;
        }
        for (StuckProjectile projectile : incoming) {
            if (projectile == null || projectile.stack().isEmpty()) {
                continue;
            }
            if (projectiles.size() >= StuckProjectile.MAX_PER_ENTITY) {
                break;
            }
            projectiles.add(projectile);
        }
    }

    @Override
    public void add(StuckProjectile projectile) {
        if (projectile == null || projectile.stack().isEmpty()) {
            return;
        }
        while (projectiles.size() >= StuckProjectile.MAX_PER_ENTITY) {
            projectiles.remove(0);
        }
        projectiles.add(projectile);
    }

    @Override
    public boolean remove(int id) {
        return projectiles.removeIf(projectile -> projectile.id() == id);
    }

    @Override
    public void clear() {
        projectiles.clear();
    }

    @Override
    public int size() {
        return projectiles.size();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (StuckProjectile projectile : projectiles) {
            list.add(projectile.save());
        }
        tag.put(TAG_PROJECTILES, list);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        projectiles.clear();
        if (nbt == null) {
            return;
        }
        ListTag list = nbt.getList(TAG_PROJECTILES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            StuckProjectile projectile = StuckProjectile.load(list.getCompound(i));
            if (projectile.stack().isEmpty()) {
                continue;
            }
            if (projectiles.size() >= StuckProjectile.MAX_PER_ENTITY) {
                break;
            }
            projectiles.add(projectile);
        }
    }
}
