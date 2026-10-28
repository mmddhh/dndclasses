package mdh.dndclasses.weapon;

import mdh.dndclasses.gear.StuckProjectile;
import mdh.dndclasses.gear.StuckProjectileEvents;
import mdh.dndclasses.gear.StuckProjectiles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Thrown weapon / launched ammunition (javelin, dart, sling bullet, blowgun needle, ...).
 * <p>
 * On an entity hit the entity is destroyed and its item + body-relative insertion data are stored
 * on the target (rendered by a client render layer, dropped on death). Block hits keep the vanilla
 * stuck-in-ground behaviour.
 */
public class ThrownWeaponEntity extends AbstractArrow {
    public static final float SPIN_DEGREES_PER_TICK = 40.0F;

    /** Only used for block hits now (stops the spin in the renderer). */
    private static final EntityDataAccessor<Boolean> DATA_STUCK =
            SynchedEntityData.defineId(ThrownWeaponEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> DATA_DISPLAY_STACK =
            SynchedEntityData.defineId(ThrownWeaponEntity.class, EntityDataSerializers.ITEM_STACK);

    private ItemStack thrownStack = ItemStack.EMPTY;
    private String rangedWeaponId = "";

    public ThrownWeaponEntity(EntityType<? extends ThrownWeaponEntity> type, Level level) {
        super(type, level);
        this.pickup = Pickup.ALLOWED;
    }

    public ThrownWeaponEntity(Level level, LivingEntity owner, ItemStack stack) {
        super(WeaponEntities.THROWN_WEAPON.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
        this.thrownStack = stack.copy();
        this.entityData.set(DATA_DISPLAY_STACK, stack.copy());
        this.pickup = Pickup.ALLOWED;
    }

    public ThrownWeaponEntity(Level level, LivingEntity owner, ItemStack displayStack, DndWeaponSpec rangedSpec) {
        this(level, owner, displayStack);
        this.rangedWeaponId = rangedSpec.id();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STUCK, false);
        this.entityData.define(DATA_DISPLAY_STACK, ItemStack.EMPTY);
    }

    public boolean isStuckForRender() {
        return this.entityData.get(DATA_STUCK);
    }

    public ItemStack getDisplayStack() {
        return this.entityData.get(DATA_DISPLAY_STACK);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) {
            // The server already stored the projectile and removed the entity; drop the client copy.
            discard();
            return;
        }

        Entity target = result.getEntity();
        if (!(target instanceof LivingEntity livingTarget)) {
            super.onHitEntity(result);
            return;
        }

        DndWeaponSpec spec = resolveSpec();
        ItemStack stuckStack = getDisplayStack();
        if (stuckStack.isEmpty()) {
            stuckStack = getPickupItem();
        }

        // Store first, damage second: a killing blow must still drop the weapon.
        StuckProjectiles.attach(livingTarget, stuckStack, result.getLocation(), getDeltaMovement(),
                StuckProjectile.Kind.ITEM);

        if (spec != null) {
            if ("net".equals(spec.type())) {
                applyNetRestraint(livingTarget);
            } else {
                Entity owner = getOwner();
                int modifier = owner instanceof Player player ? DndAttributes.modifier(player, spec.abilityMode()) : 0;
                int damage = DndCombat.minimumDamage(spec.oneHandDamage() + modifier);
                livingTarget.hurt(ModDamageTypes.source(level(), spec.damageType(), this, owner), damage);
            }
        }

        if (livingTarget.isAlive()) {
            StuckProjectileEvents.sync(livingTarget);
        }

        playSound(getHitGroundSoundEvent(), 1.0F, 1.2F / (random.nextFloat() * 0.2F + 0.9F));
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // Pure vanilla handling: the arrow parks itself exactly where it hit.
        super.onHitBlock(result);
        if (!level().isClientSide) {
            this.entityData.set(DATA_STUCK, true);
            this.pickup = Pickup.ALLOWED;
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        return thrownStack.isEmpty() ? new ItemStack(WeaponItems.weapon("iron_dart").get()) : thrownStack.copy();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!thrownStack.isEmpty()) {
            tag.put("ThrownStack", thrownStack.save(new CompoundTag()));
        }
        tag.putString("RangedWeaponId", rangedWeaponId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ThrownStack")) {
            thrownStack = ItemStack.of(tag.getCompound("ThrownStack"));
            this.entityData.set(DATA_DISPLAY_STACK, thrownStack.copy());
        }
        rangedWeaponId = tag.getString("RangedWeaponId");
        this.entityData.set(DATA_STUCK, this.inGround);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide && isStuckForRender() && player.addItem(getPickupItem())) {
            discard();
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    private DndWeaponSpec resolveSpec() {
        if (!rangedWeaponId.isEmpty() && WeaponItems.weapon(rangedWeaponId) != null) {
            return WeaponItems.specOf(WeaponItems.weapon(rangedWeaponId).get());
        }
        if (thrownStack.getItem() instanceof DndWeapon weaponItem) {
            return weaponItem.spec();
        }
        return null;
    }

    private static void applyNetRestraint(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 255, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 4, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, true));
    }
}
