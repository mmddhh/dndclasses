package mdh.dndclasses.gear;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * One projectile that is stuck inside a living entity.
 * <p>
 * {@code offset} and {@code dir} live in the entity's <em>body space</em>: origin at the feet
 * (centre of the bounding box), +Y up, +Z the direction the body faces, +X completing a
 * right-handed basis. Use {@link #worldToBody} / {@link #bodyToWorld} to convert; both are exact
 * inverses of each other and of the rotation a render layer ends up with after
 * {@code LivingEntityRenderer}'s model transform.
 * <p>
 * The data is intentionally render-agnostic: no bone, no model part, no scale. It only relies on
 * the entity's body yaw, so any {@code LivingEntity} works (see the "root transform only" rule).
 */
public record StuckProjectile(int id, ItemStack stack, Vec3 offset, Vec3 dir, Kind kind) {

    /** Hard cap per entity: keeps the NBT and the per-frame draws bounded. */
    public static final int MAX_PER_ENTITY = 64;

    /**
     * Which mesh to draw: ARROW uses the vanilla 3D arrow model (bow/crossbow ammunition), ITEM
     * uses the item sprite (thrown weapons and other ammunition, which have no 3D model).
     */
    public enum Kind {
        ARROW,
        ITEM
    }

    private static final String TAG_ID = "Id";
    private static final String TAG_ITEM = "Item";
    private static final String TAG_OFFSET = "Offset";
    private static final String TAG_DIR = "Dir";
    private static final String TAG_KIND = "Kind";

    /** Rotates a world-space vector into the body space of an entity with {@code bodyYawDegrees}. */
    public static Vec3 worldToBody(Vec3 world, float bodyYawDegrees) {
        double yaw = Math.toRadians(bodyYawDegrees);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        return new Vec3(
                world.x * cos + world.z * sin,
                world.y,
                -world.x * sin + world.z * cos);
    }

    /** Rotates a body-space vector back into world space (inverse of {@link #worldToBody}). */
    public static Vec3 bodyToWorld(Vec3 body, float bodyYawDegrees) {
        double yaw = Math.toRadians(bodyYawDegrees);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        return new Vec3(
                body.x * cos - body.z * sin,
                body.y,
                body.x * sin + body.z * cos);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_ID, id);
        tag.put(TAG_ITEM, stack.save(new CompoundTag()));
        tag.putDouble(TAG_OFFSET + "X", offset.x);
        tag.putDouble(TAG_OFFSET + "Y", offset.y);
        tag.putDouble(TAG_OFFSET + "Z", offset.z);
        tag.putDouble(TAG_DIR + "X", dir.x);
        tag.putDouble(TAG_DIR + "Y", dir.y);
        tag.putDouble(TAG_DIR + "Z", dir.z);
        tag.putByte(TAG_KIND, (byte) (kind == null ? Kind.ITEM.ordinal() : kind.ordinal()));
        return tag;
    }

    /** Never throws: unknown items deserialize to an empty stack and are filtered out by the storage. */
    public static StuckProjectile load(CompoundTag tag) {
        ItemStack stack = tag.contains(TAG_ITEM) ? ItemStack.of(tag.getCompound(TAG_ITEM)) : ItemStack.EMPTY;
        Vec3 offset = new Vec3(
                tag.getDouble(TAG_OFFSET + "X"),
                tag.getDouble(TAG_OFFSET + "Y"),
                tag.getDouble(TAG_OFFSET + "Z"));
        Vec3 dir = new Vec3(
                tag.getDouble(TAG_DIR + "X"),
                tag.getDouble(TAG_DIR + "Y"),
                tag.getDouble(TAG_DIR + "Z"));
        Kind[] kinds = Kind.values();
        int ordinal = tag.getByte(TAG_KIND);
        Kind kind = ordinal >= 0 && ordinal < kinds.length ? kinds[ordinal] : Kind.ITEM;
        return new StuckProjectile(tag.getInt(TAG_ID), stack, offset, dir, kind);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(id);
        buf.writeItem(stack);
        buf.writeDouble(offset.x);
        buf.writeDouble(offset.y);
        buf.writeDouble(offset.z);
        buf.writeDouble(dir.x);
        buf.writeDouble(dir.y);
        buf.writeDouble(dir.z);
        buf.writeByte(kind == null ? Kind.ITEM.ordinal() : kind.ordinal());
    }

    public static StuckProjectile read(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        ItemStack stack = buf.readItem();
        Vec3 offset = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 dir = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Kind[] kinds = Kind.values();
        int ordinal = buf.readByte();
        Kind kind = ordinal >= 0 && ordinal < kinds.length ? kinds[ordinal] : Kind.ITEM;
        return new StuckProjectile(id, stack, offset, dir, kind);
    }
}
