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
        target += plains * (TerrainModel.SEA_LEVEL + 4.0D + relief * 0.06D);
        target += hills * (TerrainModel.SEA_LEVEL + 10.0D + relief * 0.45D);
        target += plateau * (TerrainModel.SEA_LEVEL + 74.0D + relief * 0.12D);
        target += basin * (TerrainModel.SEA_LEVEL + 20.0D - Math.min(10.0D, relief * 0.10D));
        target += mountains * (TerrainModel.SEA_LEVEL + 105.0D + (relief - 135.0D) * 0.55D);
        return (int) Math.round(target / weight);
    }

    public double roughness(int x, int z) {
        return switch (type(x, z)) {
            case OCEAN -> 0.7D;
            case PLAINS -> 0.20D;
            case HILLS -> 0.65D;
            case PLATEAU -> 0.30D;
            case BASIN -> 0.25D;
            case MOUNTAINS -> 1.0D;
        };
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = Math.max(0.0D, Math.min(1.0D, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0D - 2.0D * t);
    }
}
