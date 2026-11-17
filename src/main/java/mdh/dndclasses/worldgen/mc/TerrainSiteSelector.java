package mdh.dndclasses.worldgen.mc;

import mdh.dndclasses.worldgen.WorldPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Coarse M2 site pass. It is intentionally simple and command-driven:
 * <ol>
 *   <li>find a land anchor by spiralling out from the world origin;</li>
 *   <li>lay the existing nation/settlement grid around that anchor;</li>
 *   <li>snap each settlement to a nearby dry site.</li>
 * </ol>
 * A proper suitability score (water, resources, slope, spacing) can replace the candidate grid
 * later without changing the WorldPlan shape.
 */
public final class TerrainSiteSelector {

    private static final int ANCHOR_RADIUS = 4096;
    private static final int ANCHOR_STEP = 256;
    private static final int SETTLEMENT_RADIUS = 256;

    private TerrainSiteSelector() {
    }

    public static WorldPlan resite(ServerLevel level, WorldPlan plan) {
        BlockPos anchor = findLand(level, 0, 0, ANCHOR_RADIUS, ANCHOR_STEP);
        if (anchor == null) {
            return plan;
        }
        List<WorldPlan.Settlement> adjusted = new ArrayList<>(plan.settlements().size());
        for (WorldPlan.Settlement settlement : plan.settlements()) {
            int column = settlement.nationId() - 3;
            int row = settlement.id() % 5;
            int candidateX = anchor.getX() + column * 320 + (row % 2) * 120;
            int candidateZ = anchor.getZ() + (row / 2) * 260 - 130;
            BlockPos site = SettlementSiteSampler.findSite(level, candidateX, candidateZ, SETTLEMENT_RADIUS);
            if (site == null) {
                site = SettlementSiteSampler.findSite(level, settlement.x(), settlement.z(), 256);
            }
            if (site == null) {
                adjusted.add(settlement);
                continue;
            }
            adjusted.add(new WorldPlan.Settlement(settlement.id(), settlement.name(),
                    settlement.nationId(), site.getX(), site.getZ(), settlement.population(),
                    settlement.culture(), settlement.materials()));
        }
        return new WorldPlan(plan.seed(), plan.nations(), List.copyOf(adjusted), plan.events());
    }

    private static BlockPos findLand(ServerLevel level, int centerX, int centerZ, int maxRadius, int step) {
        // First pass: sample the biome source without generating chunks. This can find a distant
        // continent even when the whole area around 0,0 is ocean.
        try {
            ChunkGenerator generator = level.getChunkSource().getGenerator();
            BiomeSource biomeSource = generator.getBiomeSource();
            Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
            int quartY = 64 >> 2;
            for (int radius = 0; radius <= 8192; radius += 256) {
                for (int dx = -radius; dx <= radius; dx += 256) {
                    for (int dz = -radius; dz <= radius; dz += 256) {
                        if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                            continue;
                        }
                        int x = centerX + dx;
                        int z = centerZ + dz;
                        Holder<Biome> biome = biomeSource.getNoiseBiome(x >> 2, quartY, z >> 2, sampler);
                        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) {
                            continue;
                        }
                        BlockPos dry = drySurface(level, x, z);
                        if (dry != null) {
                            return dry;
                        }
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // Fall through to the block scan below.
        }
        for (int radius = 0; radius <= maxRadius; radius += step) {
            for (int dx = -radius; dx <= radius; dx += step) {
                for (int dz = -radius; dz <= radius; dz += step) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    BlockPos dry = drySurface(level, centerX + dx, centerZ + dz);
                    if (dry != null) {
                        return dry;
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos drySurface(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight() + 1) {
            return null;
        }
        BlockPos surface = new BlockPos(x, y, z);
        BlockState below = level.getBlockState(surface.below());
        BlockState at = level.getBlockState(surface);
        BlockState above = level.getBlockState(surface.above());
        if (!below.isAir() && below.getFluidState().isEmpty()
                && at.isAir() && above.isAir()) {
            return surface;
        }
        return null;
    }
}
