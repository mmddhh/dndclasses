package mdh.dndclasses.worldgen;

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

    private static final int MAX_RADIUS = 96;
    private static final int STEP = 16;

    private SettlementSiteSampler() {
    }

    public static WorldPlan sample(ServerLevel level, WorldPlan plan) {
        List<WorldPlan.Settlement> adjusted = new ArrayList<>(plan.settlements().size());
        int moved = 0;
        for (WorldPlan.Settlement settlement : plan.settlements()) {
            BlockPos site = findSite(level, settlement.x(), settlement.z());
            if (site == null) {
                adjusted.add(settlement);
                continue;
            }
            if (site.getX() != settlement.x() || site.getZ() != settlement.z()) {
                moved++;
            }
            adjusted.add(new WorldPlan.Settlement(settlement.id(), settlement.name(),
                    settlement.nationId(), site.getX(), site.getZ(), settlement.population(),
                    settlement.culture(), settlement.materials()));
        }
        if (moved == 0) {
            return plan;
        }
        return new WorldPlan(plan.seed(), plan.nations(), List.copyOf(adjusted), plan.events());
    }

    private static BlockPos findSite(ServerLevel level, int centerX, int centerZ) {
        for (int radius = 0; radius <= MAX_RADIUS; radius += STEP) {
            for (int dx = -radius; dx <= radius; dx += STEP) {
                for (int dz = -radius; dz <= radius; dz += STEP) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos surface = new BlockPos(x, y, z);
                    BlockState below = level.getBlockState(surface.below());
                    if (!below.isAir() && below.getFluidState().isEmpty()) {
                        return surface;
                    }
                }
            }
        }
        return null;
    }
}
