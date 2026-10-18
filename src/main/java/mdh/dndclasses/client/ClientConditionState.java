package mdh.dndclasses.client;

import mdh.dndclasses.conditions.ConditionFlag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Client-side cache of the player's active conditions (for the movement mixin and a future HUD). */
@OnlyIn(Dist.CLIENT)
public final class ClientConditionState {

    public static final class Entry {
        public final ResourceLocation id;
        public final long flags;
        public final int durationTicks;
        public final UUID source;

        Entry(ResourceLocation id, long flags, int durationTicks, UUID source) {
            this.id = id;
            this.flags = flags;
            this.durationTicks = durationTicks;
            this.source = source;
        }

        public boolean has(ConditionFlag flag) {
            return (flags & (1L << flag.ordinal())) != 0L;
        }
    }

    private static List<Entry> active = new ArrayList<>();
    private static int exhaustionLevel = 0;

    private ClientConditionState() {
    }

    public static void apply(CompoundTag nbt) {
        active = new ArrayList<>();
        exhaustionLevel = nbt == null ? 0 : nbt.getInt("exhaustion");
        if (nbt == null || !nbt.contains("conditions")) {
            return;
        }
        ListTag list = nbt.getList("conditions", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
            if (id == null) {
                continue;
            }
            UUID source = tag.contains("source") && !tag.getString("source").isEmpty()
                    ? UUID.fromString(tag.getString("source")) : null;
            active.add(new Entry(id, tag.getLong("flags"), tag.getInt("duration"), source));
        }
    }

    public static List<Entry> getActive() {
        return active;
    }

    public static int getExhaustionLevel() {
        return exhaustionLevel;
    }

    public static boolean hasFlag(ConditionFlag flag) {
        for (Entry entry : active) {
            if (entry.has(flag)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isImmobile() {
        return hasFlag(ConditionFlag.IMMOBILE);
    }

    public static UUID approachSource() {
        for (Entry entry : active) {
            if (entry.has(ConditionFlag.CANNOT_APPROACH_SOURCE) && entry.source != null) {
                return entry.source;
            }
        }
        return null;
    }
}
