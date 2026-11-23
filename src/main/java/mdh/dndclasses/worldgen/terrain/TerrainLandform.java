package mdh.dndclasses.worldgen.terrain;

/**
 * Regional landform zoning. Zone boundaries use the low-frequency regional height, then blend
 * continuously so plains, plateaus, basins, hills and mountains do not switch per column.
 */
public final class TerrainLandform {

    public enum Type {
        OCEAN, PLAINS, PLATEAU, BASIN, HILLS, MOUNTAINS
    }

    private final TerrainModel model;

    public TerrainLandform(TerrainModel model) {
        this.model = model;
    }

    public Type type(int x, int z) {
        int regional = model.regionalHeight(x, z);
        if (regional <= TerrainModel.SEA_LEVEL) {
            return Type.OCEAN;
        }
        int relief = regional - TerrainModel.SEA_LEVEL;
        if (relief < 24) return Type.PLAINS;
        if (relief < 62) return Type.HILLS;
        if (relief < 118) return Type.PLATEAU;
        if (relief < 150) return Type.BASIN;
        return Type.MOUNTAINS;
    }

    /** Continuous target elevation, blended between landform bands. */
    public int targetHeight(int x, int z) {
        int regional = model.regionalHeight(x, z);
        if (regional <= TerrainModel.SEA_LEVEL) {
            return regional;
        }
        double relief = regional - TerrainModel.SEA_LEVEL;
        double plains = 1.0D - smoothstep(18.0D, 48.0D, relief);
        double hills = smoothstep(18.0D, 48.0D, relief) * (1.0D - smoothstep(70.0D, 115.0D, relief));
        double plateau = smoothstep(70.0D, 115.0D, relief) * (1.0D - smoothstep(135.0D, 175.0D, relief));
        double mountains = smoothstep(135.0D, 175.0D, relief);
        double basin = smoothstep(20.0D, 45.0D, relief) * (1.0D - smoothstep(55.0D, 80.0D, relief));
        double weight = Math.max(0.0001D, plains + hills + plateau + mountains + basin);

        double target = 0.0D;
        target += plains * (TerrainModel.SEA_LEVEL + 4.0D);
        target += hills * (TerrainModel.SEA_LEVEL + 8.0D + relief * 0.35D);
        target += plateau * (TerrainModel.SEA_LEVEL + 64.0D + relief * 0.18D);
        target += basin * (TerrainModel.SEA_LEVEL + 20.0D - Math.min(8.0D, relief * 0.08D));
        target += mountains * (TerrainModel.SEA_LEVEL + 90.0D + (relief - 135.0D) * 0.50D);
        return (int) Math.round(target / weight);
    }

    /** Continuous roughness; no hard switch at a landform boundary. */
    public double roughness(int x, int z) {
        int regional = model.regionalHeight(x, z);
        double relief = Math.max(0.0D, regional - TerrainModel.SEA_LEVEL);
        double hills = smoothstep(18.0D, 48.0D, relief);
        double plateau = smoothstep(70.0D, 115.0D, relief);
        double mountains = smoothstep(120.0D, 170.0D, relief);
        return Math.min(1.0D, 0.08D + hills * 0.48D + plateau * 0.18D + mountains * 0.40D);
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = Math.max(0.0D, Math.min(1.0D, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0D - 2.0D * t);
    }
}
