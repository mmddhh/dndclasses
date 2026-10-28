package mdh.dndclasses.weapon;

import mdh.dndclasses.gear.StuckProjectile;
import mdh.dndclasses.gear.StuckProjectileEvents;
import mdh.dndclasses.gear.StuckProjectiles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

/**
 * Arrow used by D&amp;D bows/crossbows. Damage is the fixed D&amp;D value (never scaled by
 * velocity) and picks up the matching D&amp;D ammo item. Beyond the weapon's normal range the
 * flight path receives random deviation instead of losing damage.
 * <p>
 * On an entity hit the arrow entity is destroyed and its item + body-relative insertion data are
 * stored on the target (see {@link StuckProjectiles}); the client draws it as a render layer.
 */
public class DndArrowEntity extends Arrow {

    private static final String TAG_WEAPON_ID = "DndWeaponId";
    private static final String TAG_AMMO = "DndAmmo";
    private static final String TAG_ORIGIN = "DndOrigin";

    private ItemStack ammo = ItemStack.EMPTY;
    private String weaponId = "";
    private Vec3 origin;

    public DndArrowEntity(EntityType<? extends DndArrowEntity> type, Level level) {
        super(type, level);
    }

    public DndArrowEntity(Level level, LivingEntity owner, ItemStack ammo, DndWeaponSpec spec) {
        super(WeaponEntities.DND_ARROW.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
        this.ammo = ammo.copy();
        this.ammo.setCount(1);
        this.weaponId = spec.id();
        this.pickup = Pickup.ALLOWED;
        this.setShotFromCrossbow(spec.isCrossbow());
    }

    @Override
    public void tick() {
        if (origin == null) {
            origin = position();
        }
        super.tick();

        if (!level().isClientSide && !this.inGround) {
            applyRangeDeviation();
        }
    }

    private void applyRangeDeviation() {
        DndWeaponSpec spec = spec();
        if (spec == null || spec.range() <= 0) {
            return;
        }
        double normalRange = spec.rangeBlocks();
        double longRange = spec.longRangeBlocks();
        double travelled = origin.distanceTo(position());
        if (travelled <= normalRange) {
            return;
        }
        double over = Mth.clamp((travelled - normalRange) / Math.max(1.0D, longRange - normalRange), 0.0D, 1.0D);
        if (tickCount % 4 != 0) {
            return;
        }
        double spread = 0.02D + 0.08D * over;
        Vec3 velocity = getDeltaMovement();
        setDeltaMovement(velocity.add(
                (random.nextDouble() - 0.5D) * 2.0D * spread,
                (random.nextDouble() - 0.5D) * spread * 0.5D,
                (random.nextDouble() - 0.5D) * 2.0D * spread));
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) {
            // The server already stored the projectile and removed the entity; drop the client copy.
            discard();
            return;
        }

        Entity target = result.getEntity();
        if (!(target instanceof LivingEntity living)) {
            super.onHitEntity(result);
            return;
        }
        DndWeaponSpec spec = spec();
        if (spec == null) {
            super.onHitEntity(result);
            return;
        }

        int modifier = getOwner() instanceof Player player
                ? DndAttributes.modifier(player, spec.abilityMode())
                : 0;
        boolean crit = random.nextFloat() < 0.05F
                || (spec.material() == DndWeaponMaterial.ORICHALCUM && DndCombat.isConstruct(living));
        int damage = DndCombat.minimumDamage(
                crit ? spec.oneHandDamage() * 2 + modifier : spec.oneHandDamage() + modifier);

        // Store first, damage second: a killing blow must still drop the arrow (LivingDropsEvent
        // reads the list while the target dies inside hurt()).
        StuckProjectiles.attach(living, getPickupItem(), result.getLocation(), getDeltaMovement(),
                StuckProjectile.Kind.ARROW);

        living.hurt(ModDamageTypes.source(level(), spec.damageType(), this, getOwner()), damage);
        if (living.isAlive()) {
            StuckProjectileEvents.sync(living);
        }

        if (crit && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                    12, 0.3D, 0.3D, 0.3D, 0.1D);
        }

        playSound(getHitGroundSoundEvent(), 1.0F, 1.2F / (random.nextFloat() * 0.2F + 0.9F));
        discard();
    }

    @Override
    protected ItemStack getPickupItem() {
        return ammo.isEmpty() ? new ItemStack(Items.ARROW) : ammo.copy();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(TAG_WEAPON_ID, weaponId);
        if (!ammo.isEmpty()) {
            tag.put(TAG_AMMO, ammo.save(new CompoundTag()));
        }
        if (origin != null) {
            tag.putDouble(TAG_ORIGIN + "X", origin.x);
            tag.putDouble(TAG_ORIGIN + "Y", origin.y);
            tag.putDouble(TAG_ORIGIN + "Z", origin.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        weaponId = tag.getString(TAG_WEAPON_ID);
        if (tag.contains(TAG_AMMO)) {
            ammo = ItemStack.of(tag.getCompound(TAG_AMMO));
        }
        if (tag.contains(TAG_ORIGIN + "X")) {
            origin = new Vec3(tag.getDouble(TAG_ORIGIN + "X"),
                    tag.getDouble(TAG_ORIGIN + "Y"),
                    tag.getDouble(TAG_ORIGIN + "Z"));
        }
    }

    private DndWeaponSpec spec() {
        if (weaponId.isEmpty()) {
            return null;
        }
        RegistryObject<? extends net.minecraft.world.item.Item> weapon = WeaponItems.weapon(weaponId);
        return weapon == null ? null : WeaponItems.specOf(weapon.get());
    }
}
