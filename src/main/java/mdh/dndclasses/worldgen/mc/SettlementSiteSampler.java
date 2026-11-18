package mdh.dndclasses.worldgen.mc;

import mdh.dndclasses.worldgen.WorldPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter that nudges planned settlements onto actual terrain. This is intentionally explicit
 * (a command) rather than automatic: sampling chunks can force world generation, so it must not run
 * during every plan generation.
 */
public final class SettlementSiteSampler {

    private static final int MAX_RADIUS = 192;
    private static final int STEP = 16;

    private SettlementSiteSampler() {
    }

    public static WorldPlan filterUnplaceable(ServerLevel level, WorldPlan plan) {
        java.util.Map<Integer, Integer> idMap = new java.util.HashMap<>();
        List<WorldPlan.Settlement> kept = new ArrayList<>();
        for (WorldPlan.Settlement settlement : plan.settlements()) {
            if (findSite(level, settlement.x(), settlement.z(), MAX_RADIUS) == null) {
                continue;
            }
            int newId = kept.size();
            idMap.put(settlement.id(), newId);
            kept.add(new WorldPlan.Settlement(newId, settlement.name(), settlement.nationId(),
                    settlement.x(), settlement.z(), settlement.population(), settlement.culture(),
                    settlement.materials()));
        }
        if (kept.size() == plan.settlements().size()) {
            return plan;
        }
        List<WorldPlan.HistoryEvent> events = new ArrayList<>();
        for (WorldPlan.HistoryEvent event : plan.events()) {
            Integer settlementId = idMap.get(event.settlementId());
            if (settlementId == null) {
                continue;
            }
            events.add(new WorldPlan.HistoryEvent(event.year(), event.type(), event.nationA(),
                    event.nationB(), settlementId, event.summary()));
        }
        return new WorldPlan(plan.seed(), plan.nations(), List.copyOf(kept),
                List.copyOf(events));
    }

    public static WorldPlan sample(ServerLevel level, WorldPlan plan) {
        List<WorldPlan.Settlement> adjusted = new ArrayList<>(plan.settlements().size());
        int moved = 0;
        for (WorldPlan.Settlement settlement : plan.settlements()) {
            BlockPos site = findSite(level, settlement.x(), settlement.z(), MAX_RADIUS);
            if (site == null) {
                adjusted.add(settlement);
                continue;
            }
            if (site.getX() != settlement.x() || site.getZ() != settlement.z()) {
                moved++;
            }
            adjusted.add(relocated(settlement, site));
        }
        if (moved == 0) {
            return plan;
        }
        return new WorldPlan(plan.seed(), plan.nations(), List.copyOf(adjusted), plan.events());
    }

    /** Correct one settlement against a larger search radius; used by /worldplan tp. */
    public static WorldPlan relocate(ServerLevel level, WorldPlan plan, int settlementId, int maxRadius) {
        WorldPlan.Settlement target = plan.settlements().stream()
                .filter(candidate -> candidate.id() == settlementId).findFirst().orElse(null);
        if (target == null) {
            return plan;
        }
        BlockPos site = findSite(level, target.x(), target.z(), maxRadius);
        if (site == null || (site.getX() == target.x() && site.getZ() == target.z())) {
            return plan;
        }
        List<WorldPlan.Settlement> adjusted = new ArrayList<>(plan.settlements().size());
        for (WorldPlan.Settlement settlement : plan.settlements()) {
            adjusted.add(settlement.id() == settlementId ? relocated(settlement, site) : settlement);
        }
        return new WorldPlan(plan.seed(), plan.nations(), List.copyOf(adjusted), plan.events());
    }

    private static WorldPlan.Settlement relocated(WorldPlan.Settlement settlement, BlockPos site) {
        return new WorldPlan.Settlement(settlement.id(), settlement.name(),
                settlement.nationId(), site.getX(), site.getZ(), settlement.population(),
                settlement.culture(), settlement.materials());
    }

    public static BlockPos findSite(ServerLevel level, int centerX, int centerZ, int maxRadius) {
        for (int radius = 0; radius <= maxRadius; radius += STEP) {
            for (int dx = -radius; dx <= radius; dx += STEP) {
                for (int dz = -radius; dz <= radius; dz += STEP) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    if (y <= level.getMinBuildHeight() + 1) {
                        continue;
                    }
                    BlockPos surface = new BlockPos(x, y, z);
                    BlockState below = level.getBlockState(surface.below());
                    BlockState at = level.getBlockState(surface);
                    if (!below.isAir() && below.getFluidState().isEmpty()
                            && at.getFluidState().isEmpty()) {
                        return surface;
                    }
                }
            }
        }
        return null;
    }
}
