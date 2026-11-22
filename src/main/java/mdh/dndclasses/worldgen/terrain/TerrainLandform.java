package mdh.dndclasses.worldgen.terrain;

/**
 * Landform classification independent from biome. It turns the broad continental height into a
 * target elevation for plains, plateaus, basins, hills and mountains so each landform can use a
 * different surface function.
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
        int height = model.height(x, z);
        if (height <= TerrainModel.SEA_LEVEL) {
            return Type.OCEAN;
        }
        int relief = height - TerrainModel.SEA_LEVEL;
        if (relief < 14) {
            return Type.PLAINS;
        }
        if (relief < 45) {
            return Type.HILLS;
        }
        if (relief < 105) {
            return Type.PLATEAU;
        }
        if (relief < 145) {
            return Type.BASIN;
        }
        return Type.MOUNTAINS;
    }

    /**
     * Target surface height. Plains are nearly flat, plateaus are high and flat, basins are
     * shallow bowls, hills retain medium relief, mountains keep the ridged base.
     */
    public int targetHeight(int x, int z) {
        int base = model.height(x, z);
        int relief = Math.max(0, base - TerrainModel.SEA_LEVEL);
        return switch (type(x, z)) {
            case OCEAN -> base;
            case PLAINS -> TerrainModel.SEA_LEVEL + 4 + (relief / 6);
            case HILLS -> TerrainModel.SEA_LEVEL + 8 + (relief / 2);
            case PLATEAU -> TerrainModel.SEA_LEVEL + 86 + (relief / 8);
            case BASIN -> TerrainModel.SEA_LEVEL + 22 - Math.min(14, relief / 10);
            case MOUNTAINS -> TerrainModel.SEA_LEVEL + 120 + (relief * 2 / 3);
        };
    }
}
