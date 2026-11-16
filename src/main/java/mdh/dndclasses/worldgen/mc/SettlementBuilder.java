package mdh.dndclasses.worldgen.mc;

import mdh.dndclasses.worldgen.WorldPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
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

        String material = chooseMaterial(level, new Site(centerX, y, centerZ), settlement);
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

        String roofType = cultureValue(settlement.culture(), "roof", "flat");
        String windowType = cultureValue(settlement.culture(), "window", "small");
        String motif = cultureValue(settlement.culture(), "motif", "rune");
        int wallHeight = switch (roofType) {
            case "spiral" -> 5;
            case "steep" -> 4;
            case "flat" -> 2;
            default -> 3;
        };

        // Clear a 9x9 volume tall enough for the selected roof.
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = 1; dy <= wallHeight + 6; dy++) {
                    placed += set(level, centerX + dx, y + dy, centerZ + dz,
                            Blocks.AIR.defaultBlockState());
                }
                placed += set(level, centerX + dx, y, centerZ + dz, foundation);
            }
        }

        // Walls, windows and a 1x2 doorway on the south face.
        for (int dy = 1; dy <= wallHeight; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    boolean edge = Math.abs(dx) == 4 || Math.abs(dz) == 4;
                    if (!edge) {
                        continue;
                    }
                    boolean door = dz == -4 && dx == 0 && dy <= 2;
                    boolean window = isWindow(dx, dz, dy, wallHeight, windowType);
                    if (door) {
                        BlockState doorState = Blocks.OAK_DOOR.defaultBlockState()
                                .setValue(DoorBlock.FACING, Direction.NORTH)
                                .setValue(DoorBlock.HALF, dy == 1
                                        ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
                        placed += set(level, centerX + dx, y + dy, centerZ + dz, doorState);
                    } else if (window) {
                        placed += set(level, centerX + dx, y + dy, centerZ + dz,
                                Blocks.GLASS_PANE.defaultBlockState());
                    } else {
                        placed += set(level, centerX + dx, y + dy, centerZ + dz, wall);
                    }
                }
            }
        }

        int roofBase = y + wallHeight + 1;
        switch (roofType) {
            case "gabled" -> buildGabledRoof(level, centerX, centerZ, roofBase, roof);
            case "domed" -> buildDomedRoof(level, centerX, centerZ, roofBase, roof);
            case "spiral" -> buildSpiralRoof(level, centerX, centerZ, roofBase, roof, wall);
            case "flat" -> buildFlatRoof(level, centerX, centerZ, roofBase, roof, wall);
            default -> buildSteepRoof(level, centerX, centerZ, roofBase, roof, wall);
        }

        placed += set(level, centerX, y, centerZ, motifBlock(motif));
        placed += set(level, centerX, y + 1, centerZ, Blocks.TORCH.defaultBlockState());
        return placed;
    }

    private static void buildFlatRoof(ServerLevel level, int x, int z, int base, BlockState roof,
                                      BlockState wall) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                set(level, x + dx, base, z + dz, roof);
            }
        }
        for (int dx : new int[]{-4, 4}) {
            for (int dz : new int[]{-4, 4}) {
                set(level, x + dx, base + 1, z + dz, wall);
            }
        }
    }

    private static void buildSteepRoof(ServerLevel level, int x, int z, int base, BlockState roof,
                                       BlockState wall) {
        for (int layer = 0; layer <= 4; layer++) {
            int half = 4 - layer;
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    set(level, x + dx, base + layer, z + dz, roof);
                }
            }
        }
        set(level, x, base + 5, z, wall);
    }

    private static void buildGabledRoof(ServerLevel level, int x, int z, int base, BlockState roof) {
        for (int layer = 0; layer <= 4; layer++) {
            int half = 4 - layer;
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    set(level, x + dx, base + layer, z + dz, roof);
                }
            }
        }
    }

    private static void buildDomedRoof(ServerLevel level, int x, int z, int base, BlockState roof) {
        // First layer must cover the full square footprint, otherwise the corners leak.
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                set(level, x + dx, base, z + dz, roof);
            }
        }
        for (int layer = 1; layer <= 3; layer++) {
            int radius = 4 - layer;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz <= radius * radius + 1) {
                        set(level, x + dx, base + layer, z + dz, roof);
                    }
                }
            }
        }
    }

    private static void buildSpiralRoof(ServerLevel level, int x, int z, int base, BlockState roof,
                                        BlockState wall) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                set(level, x + dx, base, z + dz, roof);
            }
        }
        for (int layer = 1; layer <= 5; layer++) {
            int offset = layer % 2 == 0 ? -1 : 1;
            int half = Math.max(0, 3 - layer / 2);
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    set(level, x + dx + offset, base + layer, z + dz, roof);
                }
            }
        }
        set(level, x, base + 6, z, wall);
    }

    private static boolean isWindow(int dx, int dz, int dy, int wallHeight, String style) {
        boolean wall = Math.abs(dx) == 4 || Math.abs(dz) == 4;
        if (!wall) {
            return false;
        }
        int along = Math.abs(dx) == 4 ? dz : dx;
        if (Math.abs(along) % 3 != 0 || Math.abs(along) == 0) {
            return false;
        }
        return switch (style) {
            case "tall" -> dy == 1 || dy == 2;
            case "arched" -> dy == 2 || (dy == 3 && Math.abs(along) == 3);
            case "round" -> dy == 2 && Math.abs(along) == 3;
            default -> dy == 2;
        };
    }

    private static String cultureValue(String culture, String key, String fallback) {
        if (culture == null) {
            return fallback;
        }
        for (String part : culture.split(";")) {
            int split = part.indexOf('=');
            if (split > 0 && part.substring(0, split).equals(key)) {
                return part.substring(split + 1);
            }
        }
        return fallback;
    }

    private static BlockState motifBlock(String motif) {
        return switch (motif) {
            case "rune" -> Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            case "vine" -> Blocks.OAK_LEAVES.defaultBlockState();
            case "star" -> Blocks.GLOWSTONE.defaultBlockState();
            case "dragon" -> Blocks.MAGMA_BLOCK.defaultBlockState();
            case "hammer" -> Blocks.IRON_BLOCK.defaultBlockState();
            case "wave" -> Blocks.PRISMARINE_BRICKS.defaultBlockState();
            default -> Blocks.STONE_BRICKS.defaultBlockState();
        };
    }

    private static int set(ServerLevel level, int x, int y, int z, BlockState state) {
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos) == state) {
            return 0;
        }
        return level.setBlock(pos, state, 3) ? 1 : 0;
    }

    public record Site(int x, int y, int z) {
    }

    /** Public preview for commands: the actual dry site and locally chosen material. */
    public static Site findSite(ServerLevel level, int centerX, int centerZ) {
        return findDrySite(level, centerX, centerZ);
    }

    public static String materialAt(ServerLevel level, WorldPlan.Settlement settlement) {
        Site site = findDrySite(level, settlement.x(), settlement.z());
        if (site == null) {
            return null;
        }
        return chooseMaterial(level, site, settlement);
    }

    /** Culture preference wins when locally available; otherwise use the local substitute. */
    private static String chooseMaterial(ServerLevel level, Site site, WorldPlan.Settlement settlement) {
        for (String preferred : settlement.materials()) {
            if (hasLocal(level, site.x(), site.y(), site.z(), preferred)) {
                return preferred;
            }
        }
        String local = localMaterial(level, site.x(), site.y(), site.z());
        if (local != null) {
            return local;
        }
        return settlement.materials().isEmpty() ? null : settlement.materials().get(0);
    }

    private static boolean hasLocal(ServerLevel level, int x, int y, int z, String material) {
        if ("sandstone".equals(material) && !isSandyBiome(level, x, y, z)) {
            return false;
        }
        for (int dx = -8; dx <= 8; dx += 2) {
            for (int dz = -8; dz <= 8; dz += 2) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockState state = level.getBlockState(new BlockPos(x + dx, y + dy, z + dz));
                    if (matchesMaterial(state, material)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean matchesMaterial(BlockState state, String material) {
        return switch (material) {
            case "oak" -> state.is(Blocks.OAK_LOG) || state.is(Blocks.OAK_LEAVES)
                    || state.is(Blocks.OAK_PLANKS);
            case "spruce" -> state.is(Blocks.SPRUCE_LOG) || state.is(Blocks.SPRUCE_LEAVES)
                    || state.is(Blocks.SPRUCE_PLANKS);
            case "deepslate" -> state.is(Blocks.DEEPSLATE) || state.is(Blocks.DEEPSLATE_BRICKS);
            case "sandstone" -> state.is(Blocks.SANDSTONE) || state.is(Blocks.SAND);
            case "copper" -> state.is(Blocks.COPPER_ORE) || state.is(Blocks.COPPER_BLOCK);
            case "stone" -> state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE);
            case "earth" -> state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)
                    || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)
                    || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM);
            default -> false;
        };
    }

    private static boolean isSandyBiome(ServerLevel level, int x, int y, int z) {
        Holder<Biome> biome = level.getBiome(new BlockPos(x, y, z));
        return biome.is(BiomeTags.IS_BADLANDS)
                || biome.unwrapKey().map(key -> key.location().getPath().equals("desert"))
                .orElse(false);
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
                    BlockState above = level.getBlockState(new BlockPos(x, y + 1, z));
                    if (!below.isAir() && below.getFluidState().isEmpty()
                            && at.isAir() && above.isAir()) {
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
        int earth = 0;
        for (int dx = -8; dx <= 8; dx += 2) {
            for (int dz = -8; dz <= 8; dz += 2) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockState state = level.getBlockState(new BlockPos(x + dx, y + dy, z + dz));
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
                    } else if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)
                            || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)
                            || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)) {
                        earth++;
                    }
                }
            }
        }
        int best = Math.max(Math.max(oak, spruce), Math.max(Math.max(deepslate, sandstone),
                Math.max(copper, Math.max(stone, earth))));
        if (best == 0) {
            return null;
        }
        Holder<Biome> biome = level.getBiome(new BlockPos(x, y, z));
        boolean sandyBiome = biome.is(BiomeTags.IS_BADLANDS)
                || biome.unwrapKey().map(key -> key.location().getPath().equals("desert")).orElse(false);
        if (best == sandstone && !sandyBiome) {
            // A beach/shore sample should not turn a whole settlement into sandstone.
            int next = Math.max(Math.max(oak, spruce), Math.max(Math.max(deepslate, copper),
                    Math.max(stone, earth)));
            best = next == 0 ? stone : next;
            if (best == stone) {
                return "stone";
            }
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
        if (best == earth) {
            return "earth";
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
            case "earth" -> Blocks.PACKED_MUD.defaultBlockState();
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
            case "earth" -> Blocks.MUD_BRICKS.defaultBlockState();
            default -> Blocks.STONE_BRICK_SLAB.defaultBlockState();
        };
    }
}
