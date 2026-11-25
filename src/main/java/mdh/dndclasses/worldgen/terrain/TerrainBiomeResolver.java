package mdh.dndclasses.worldgen.terrain;

/**
 * Single source of truth for surface height, landform and biome. The chunk generator and the biome
 * source must both use this resolver so F3 cannot report a mountain biome on a plain.
 */
public final class TerrainBiomeResolver {

    public record Sample(int surface, TerrainLandform.Type landform,
                         TerrainModel.Biome biome, double temperature, double humidity) {
    }

    private final TerrainModel model;
    private final TerrainDensity density;
    private final ThreadLocal<Cache> cache = ThreadLocal.withInitial(Cache::new);

    private static final class Cache {
        int x = Integer.MIN_VALUE;
        int z = Integer.MIN_VALUE;
        Sample sample;
    }

    public TerrainBiomeResolver(TerrainModel model, TerrainDensity density) {
        this.model = model;
        this.density = density;
    }

    public Sample sample(int x, int z) {
        Cache local = cache.get();
        if (local.x == x && local.z == z && local.sample != null) {
            return local.sample;
        }
        int surface = density.surfaceHeight(x, z);
        TerrainLandform.Type landform = density.landform().typeAtSurface(x, z, surface);
        TerrainModel.Biome biome = model.biome(x, z, surface, landform);
        Sample sample = new Sample(surface, landform, biome,
                model.temperature(x, z, surface), model.humidity(x, z, surface));
        local.x = x;
        local.z = z;
        local.sample = sample;
        return sample;
    }

    public TerrainModel.Biome biomeAt(int x, int z, int surface) {
        TerrainLandform.Type landform = density.landform().typeAtSurface(x, z, surface);
        return model.biome(x, z, surface, landform);
    }
}
