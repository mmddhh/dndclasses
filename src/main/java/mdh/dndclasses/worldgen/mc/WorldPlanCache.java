package mdh.dndclasses.worldgen.mc;

import mdh.dndclasses.worldgen.WorldPlan;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Immutable per-server snapshot of settlement positions for chunk-generation threads. It is filled
 * on server start from SavedData and cleared on stop; chunk workers only read it.
 */
public final class WorldPlanCache {

    private static volatile Map<Long, WorldPlan.Settlement> settlementsByChunk = Map.of();

    private WorldPlanCache() {
    }

    public static void set(WorldPlan plan) {
        Map<Long, WorldPlan.Settlement> map = new HashMap<>();
        for (WorldPlan.Settlement settlement : plan.settlements()) {
            map.put(ChunkPos.asLong(settlement.x() >> 4, settlement.z() >> 4), settlement);
        }
        settlementsByChunk = Map.copyOf(map);
    }

    public static WorldPlan.Settlement settlementAt(ChunkPos chunk) {
        return settlementsByChunk.get(ChunkPos.asLong(chunk.x, chunk.z));
    }

    public static void clear() {
        settlementsByChunk = Map.of();
    }
}
