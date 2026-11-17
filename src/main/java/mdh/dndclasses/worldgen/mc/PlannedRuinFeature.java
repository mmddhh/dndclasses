package mdh.dndclasses.worldgen.mc;

import mdh.dndclasses.worldgen.WorldPlan;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Placement bridge for the M1 prototype: runs once per chunk, looks for a planned settlement in
 * that chunk, and builds the test watchtower there. The feature is data-driven and registered to
 * overworld biomes through a biome modifier.
 */
public final class PlannedRuinFeature extends Feature<NoneFeatureConfiguration> {

    public PlannedRuinFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkPos chunk = new ChunkPos(context.origin());
        WorldPlan.Settlement settlement = WorldPlanCache.settlementAt(chunk);
        if (settlement == null || WorldPlanCache.isBuilt(settlement.id())) {
            return false;
        }
        int placed = SettlementBuilder.build(level, settlement);
        if (placed > 0) {
            WorldPlanCache.markBuilt(settlement.id());
            return true;
        }
        return false;
    }
}
