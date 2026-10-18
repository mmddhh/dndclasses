package mdh.dndclasses.weapon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.NetworkHooks;

public class ThrownWeaponEntity extends ThrowableItemProjectile {
    private ItemStack thrownStack = ItemStack.EMPTY;
    private String rangedWeaponId = "";
    private boolean stuck;
    private int stuckEntityId = -1;

    public ThrownWeaponEntity(EntityType<? extends ThrownWeaponEntity> type, Level level) {
        super(type, level);
    }

    public ThrownWeaponEntity(Level level, LivingEntity owner, ItemStack stack) {
        super(WeaponEntities.THROWN_WEAPON.get(), owner, level);
        this.thrownStack = stack.copy();
        setItem(stack.copy());
    }

    public ThrownWeaponEntity(Level level, LivingEntity owner, ItemStack displayStack, DndWeaponSpec rangedSpec) {
        this(level, owner, displayStack);
        this.rangedWeaponId = rangedSpec.id();
    }

    @Override
    protected Item getDefaultItem() {
        return WeaponItems.weapon("iron_dart").get();
    }

    @Override
    public void tick() {
        if (stuck) {
            setDeltaMovement(0, 0, 0);
            if (!level().isClientSide && stuckEntityId != -1) {
                Entity target = level().getEntity(stuckEntityId);
                if (target != null && target.isAlive()) {
                    setPos(target.getX(), target.getY(0.65D), target.getZ());
                    if (target instanceof LivingEntity livingTarget && isNet()) {
                        applyNetRestraint(livingTarget);
                    }
                } else {
                    dropStoredStack();
                    discard();
                }
            }
            return;
        }
        super.tick();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) {
            return;
        }
        Entity owner = getOwner();
        Entity target = result.getEntity();
        DndWeaponSpec spec = resolveSpec();
        if (target instanceof LivingEntity livingTarget && spec != null) {
            if ("net".equals(spec.type())) {
                applyNetRestraint(livingTarget);
            } else {
                int modifier = owner instanceof Player player ? DndAttributes.modifier(player, spec.abilityMode()) : 0;
                int damage = DndCombat.minimumDamage(spec.oneHandDamage() + modifier);
                livingTarget.hurt(ModDamageTypes.source(level(), spec.damageType(), this, owner), damage);
            }
        }
        stuck = true;
        stuckEntityId = target.getId();
        setNoGravity(true);
        setDeltaMovement(0, 0, 0);
        setPos(target.getX(), target.getY(0.65D), target.getZ());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide) {
            stuck = true;
            setNoGravity(true);
            setDeltaMovement(0, 0, 0);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide && stuck && player.addItem(getStoredStack())) {
            clearNetRestraintIfNeeded();
            discard();
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void playerTouch(Player player) {
        if (!level().isClientSide && stuck && player.addItem(getStoredStack())) {
            clearNetRestraintIfNeeded();
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!thrownStack.isEmpty()) {
            tag.put("ThrownStack", thrownStack.save(new CompoundTag()));
        }
        tag.putString("RangedWeaponId", rangedWeaponId);
        tag.putBoolean("Stuck", stuck);
        tag.putInt("StuckEntityId", stuckEntityId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ThrownStack")) {
            thrownStack = ItemStack.of(tag.getCompound("ThrownStack"));
            setItem(thrownStack.copy());
        }
        rangedWeaponId = tag.getString("RangedWeaponId");
        stuck = tag.getBoolean("Stuck");
        stuckEntityId = tag.getInt("StuckEntityId");
        setNoGravity(stuck);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private DndWeaponSpec resolveSpec() {
        if (!rangedWeaponId.isEmpty() && WeaponItems.weapon(rangedWeaponId) != null) {
            return WeaponItems.weapon(rangedWeaponId).get().spec();
        }
        if (thrownStack.getItem() instanceof DndWeaponItem weaponItem) {
            return weaponItem.spec();
        }
        return null;
    }

    private ItemStack getStoredStack() {
        if (thrownStack.isEmpty()) {
            return getItem().copy();
        }
        return thrownStack.copy();
    }

    private void dropStoredStack() {
        spawnAtLocation(getStoredStack());
    }

    private static void applyNetRestraint(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 255, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 4, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, true));
    }

    private boolean isNet() {
        DndWeaponSpec spec = resolveSpec();
        return spec != null && "net".equals(spec.type());
    }

    private void clearNetRestraintIfNeeded() {
        if (!isNet() || stuckEntityId == -1) {
            return;
        }
        Entity target = level().getEntity(stuckEntityId);
        if (target instanceof LivingEntity livingTarget) {
            livingTarget.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            livingTarget.removeEffect(MobEffects.DIG_SLOWDOWN);
            livingTarget.removeEffect(MobEffects.WEAKNESS);
        }
    }
}
