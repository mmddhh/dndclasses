package mdh.dndclasses.worldgen.terrain;

/**
 * Legacy global hydrology entry point retained for the offline map command.
 *
 * <p>Chunk generation now uses {@link RegionalHydrology}; the global service is only a view helper
 * and must not be treated as the source of river data for the generator.
 */
public final class HydrologyService {

    public record State(long seed, TerrainGrid grid) {
    }

    private static volatile State current;

    private HydrologyService() {
    }

    public static State current() {
        return current;
    }

    public static State ensure(long seed) {
        State state = current;
        if (state != null && state.seed() == seed) {
            return state;
        }
        State computed = compute(seed);
        current = computed;
        return computed;
    }

    public static void clear() {
        current = null;
        RegionalHydrology.clear();
    }

    private static State compute(long seed) {
        TerrainGrid grid = DensityMapSampler.sample(WorldTopology.standard(), seed);
        return new State(seed, grid);
    }
}
