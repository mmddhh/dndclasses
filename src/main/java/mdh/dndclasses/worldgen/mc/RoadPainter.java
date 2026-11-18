package mdh.dndclasses.worldgen.mc;

import mdh.dndclasses.worldgen.WorldPlan;
import mdh.dndclasses.worldgen.road.RoadNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Debug-only painter: places a one-block-wide road strip and the existing watchtower at every
 * settlement so an external map mod can inspect the plan immediately. This is not the final
 * chunk-generation feature.
 */
public final class RoadPainter {

    private RoadPainter() {
    }

    public static int paintRoads(ServerLevel level, RoadNetwork network) {
        int placed = 0;
        for (RoadNetwork.Road road : network.roads()) {
            BlockState state = switch (road.tier()) {
                case HIGHWAY -> Blocks.STONE_BRICKS.defaultBlockState();
                case ROAD -> Blocks.COBBLESTONE.defaultBlockState();
                case PATH -> Blocks.DIRT_PATH.defaultBlockState();
            };
            for (int i = 1; i < road.path().size(); i++) {
                RoadNetwork.Node a = road.path().get(i - 1);
                RoadNetwork.Node b = road.path().get(i);
                int distance = Math.max(Math.abs(b.x() - a.x()), Math.abs(b.z() - a.z()));
                int steps = Math.max(1, distance / 4);
                for (int s = 0; s <= steps; s++) {
                    int x = a.x() + (b.x() - a.x()) * s / steps;
                    int z = a.z() + (b.z() - a.z()) * s / steps;
                    placed += paintColumn(level, x, z, state);
                }
            }
        }
        return placed;
    }

    public static int paintSettlements(ServerLevel level, WorldPlan plan) {
        int built = 0;
        for (WorldPlan.Settlement settlement : plan.settlements()) {
            if (SettlementBuilder.build(level, settlement) > 0) {
                built++;
            }
        }
        return built;
    }

    private static int paintColumn(ServerLevel level, int x, int z, BlockState state) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight() + 1) {
            return 0;
        }
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.getBlockState(pos).isAir()) {
            return 0;
        }
        level.setBlock(pos, state, 2);
        return 1;
    }
}
