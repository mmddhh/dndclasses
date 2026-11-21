package mdh.dndclasses.worldgen.terrain;

import java.util.concurrent.CompletableFuture;

/**
 * Deterministic hydrology for a world seed. Both chunk generation and the offline map read this
 * service; the map never installs river data into the generator.
 */
public final class HydrologyService {

    public record State(long seed, TerrainGrid grid) {
    }

    private static volatile State current;
    private static volatile CompletableFuture<State> pending;

    private HydrologyService() {
    }

    public static State current() {
        return current;
    }

    /** Synchronous use for the map command and tests. */
    public static State ensure(long seed) {
        State state = current;
        if (state != null && state.seed() == seed) {
            return state;
        }
        State computed = compute(seed);
        current = computed;
        return computed;
    }

    /** Server startup path: compute off-thread, so chunk generation can run without rivers until ready. */
    public static void ensureAsync(long seed) {
        State state = current;
        if (state != null && state.seed() == seed) {
            return;
        }
        CompletableFuture<State> existing = pending;
        if (existing != null && !existing.isDone()) {
            return;
        }
        pending = CompletableFuture.supplyAsync(() -> compute(seed))
                .thenApply(computed -> {
                    current = computed;
                    return computed;
                });
    }

    public static void clear() {
        current = null;
        pending = null;
    }

    private static State compute(long seed) {
        TerrainGrid grid = DensityMapSampler.sample(WorldTopology.standard(), seed);
        RiverField.install(grid.riverPaths(), grid.topology());
        return new State(seed, grid);
    }
}
