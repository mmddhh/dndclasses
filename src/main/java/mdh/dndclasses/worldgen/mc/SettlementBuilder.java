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
        Site site = findDrySite(level, settlement.x(), settlement.z());
        if (site == null) {
            return 0;
        }
        int centerX = site.x();
        int centerZ = site.z();
        int y = site.y();

        String material = localMaterial(level, centerX, y, centerZ);
        if (material == null) {
            material = settlement.materials().isEmpty() ? "stone" : settlement.materials().get(0);
        }
        BlockState wall = wallFor(material);
        BlockState roof = roofFor(material);
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

    private record Site(int x, int y, int z) {
    }

    /** Finds dry, non-fluid ground near the planned point; null when the area is water/void. */
    private static Site findDrySite(ServerLevel level, int centerX, int centerZ) {
        for (int radius = 0; radius <= 32; radius += 4) {
            for (int dx = -radius; dx <= radius; dx += 4) {
                for (int dz = -radius; dz <= radius; dz += 4) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    if (y <= level.getMinBuildHeight() + 1) {
                        continue;
                    }
                    BlockPos ground = new BlockPos(x, y - 1, z);
                    BlockState below = level.getBlockState(ground);
                    BlockState at = level.getBlockState(new BlockPos(x, y, z));
                    if (!below.isAir() && below.getFluidState().isEmpty()
                            && at.getFluidState().isEmpty()) {
                        return new Site(x, y, z);
                    }
                }
            }
        }
        return null;
    }

    /** Picks the material actually visible around the site; null when the plan material should win. */
    private static String localMaterial(ServerLevel level, int x, int y, int z) {
        int oak = 0;
        int spruce = 0;
        int deepslate = 0;
        int sandstone = 0;
        int copper = 0;
        int stone = 0;
        for (int dx = -8; dx <= 8; dx += 2) {
            for (int dz = -8; dz <= 8; dz += 2) {
                BlockState state = level.getBlockState(new BlockPos(x + dx, y - 1, z + dz));
                if (state.is(Blocks.OAK_LOG) || state.is(Blocks.OAK_LEAVES)
                        || state.is(Blocks.OAK_PLANKS)) {
                    oak++;
                } else if (state.is(Blocks.SPRUCE_LOG) || state.is(Blocks.SPRUCE_LEAVES)
                        || state.is(Blocks.SPRUCE_PLANKS)) {
                    spruce++;
                } else if (state.is(Blocks.DEEPSLATE) || state.is(Blocks.DEEPSLATE_BRICKS)) {
                    deepslate++;
                } else if (state.is(Blocks.SANDSTONE) || state.is(Blocks.SAND)) {
                    sandstone++;
                } else if (state.is(Blocks.COPPER_ORE) || state.is(Blocks.COPPER_BLOCK)) {
                    copper++;
                } else if (state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE)) {
                    stone++;
                }
            }
        }
        int best = Math.max(Math.max(oak, spruce), Math.max(Math.max(deepslate, sandstone),
                Math.max(copper, stone)));
        if (best == 0) {
            return null;
        }
        if (best == oak) {
            return "oak";
        }
        if (best == spruce) {
            return "spruce";
        }
        if (best == deepslate) {
            return "deepslate";
        }
        if (best == sandstone) {
            return "sandstone";
        }
        if (best == copper) {
            return "copper";
        }
        return "stone";
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
            case "oak" -> Blocks.OAK_PLANKS.defaultBlockState();
            case "spruce" -> Blocks.SPRUCE_PLANKS.defaultBlockState();
            case "deepslate" -> Blocks.DEEPSLATE_TILES.defaultBlockState();
            case "sandstone" -> Blocks.CUT_SANDSTONE.defaultBlockState();
            case "copper" -> Blocks.WEATHERED_CUT_COPPER.defaultBlockState();
            default -> Blocks.STONE_BRICK_SLAB.defaultBlockState();
        };
    }
}
