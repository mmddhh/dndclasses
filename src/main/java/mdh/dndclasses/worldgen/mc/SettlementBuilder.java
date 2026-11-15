package mdh.dndclasses.worldgen.mc;

import mdh.dndclasses.worldgen.WorldPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Minimal placement adapter for the M1 prototype: builds one small watchtower at a planned
 * settlement coordinate. It is deliberately command-driven; natural structure generation will
 * replace this once the coordinate/terrain path is verified.
 */
public final class SettlementBuilder {

    private SettlementBuilder() {
    }

    public static int build(ServerLevel level, WorldPlan.Settlement settlement) {
        int centerX = settlement.x();
        int centerZ = settlement.z();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerX, centerZ);
        if (y <= level.getMinBuildHeight() + 1) {
            return 0;
        }

        BlockState wall = wallFor(settlement.materials().isEmpty() ? "stone" : settlement.materials().get(0));
        BlockState roof = roofFor(settlement.materials().size() > 1
                ? settlement.materials().get(1) : "stone");
        BlockState foundation = Blocks.STONE_BRICKS.defaultBlockState();
        int placed = 0;

        // Clear a 9x9x6 volume and lay the foundation.
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = 1; dy <= 6; dy++) {
                    placed += set(level, centerX + dx, y + dy, centerZ + dz,
                            Blocks.AIR.defaultBlockState());
                }
                placed += set(level, centerX + dx, y, centerZ + dz, foundation);
            }
        }

        // Walls, with a 1x2 doorway on the south face.
        for (int dy = 1; dy <= 3; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    boolean edge = Math.abs(dx) == 4 || Math.abs(dz) == 4;
                    if (!edge) {
                        continue;
                    }
                    boolean door = dz == -4 && dx == 0 && dy <= 2;
                    if (!door) {
                        placed += set(level, centerX + dx, y + dy, centerZ + dz, wall);
                    }
                }
            }
        }

        // Roof and corner merlons.
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                placed += set(level, centerX + dx, y + 4, centerZ + dz, roof);
            }
        }
        for (int dx : new int[]{-4, 4}) {
            for (int dz : new int[]{-4, 4}) {
                placed += set(level, centerX + dx, y + 5, centerZ + dz, wall);
            }
        }

        placed += set(level, centerX, y + 1, centerZ, Blocks.TORCH.defaultBlockState());
        return placed;
    }

    private static int set(ServerLevel level, int x, int y, int z, BlockState state) {
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos) == state) {
            return 0;
        }
        return level.setBlock(pos, state, 3) ? 1 : 0;
    }

    private static BlockState wallFor(String material) {
        return switch (material) {
            case "oak" -> Blocks.OAK_PLANKS.defaultBlockState();
            case "spruce" -> Blocks.SPRUCE_PLANKS.defaultBlockState();
            case "deepslate" -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            case "sandstone" -> Blocks.SANDSTONE.defaultBlockState();
            case "copper" -> Blocks.CUT_COPPER.defaultBlockState();
            default -> Blocks.STONE_BRICKS.defaultBlockState();
        };
    }

    private static BlockState roofFor(String material) {
        return switch (material) {
            case "oak" -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
            case "spruce" -> Blocks.SPRUCE_PLANKS.defaultBlockState();
            case "deepslate" -> Blocks.DEEPSLATE_TILES.defaultBlockState();
            case "sandstone" -> Blocks.CUT_SANDSTONE.defaultBlockState();
            case "copper" -> Blocks.WEATHERED_CUT_COPPER.defaultBlockState();
            default -> Blocks.STONE_BRICK_SLAB.defaultBlockState();
        };
    }
}
