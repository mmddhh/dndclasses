package mdh.dndclasses.worldgen.terrain;

import java.nio.file.Path;

/** Offline entry point for inspecting the first-part terrain prototype. */
public final class TerrainPrototype {

    private TerrainPrototype() {
    }

    public static void main(String[] args) throws Exception {
        WorldTopology topology = WorldTopology.standard();
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1L;
        long started = System.nanoTime();
        TerrainGrid grid = HabitabilitySampler.sample(topology, seed);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        Path output = Path.of("build", "terrain-prototype");
        grid.writeImages(output);

        int land = 0;
        int ocean = 0;
        int high = 0;
        double habitabilitySum = 0.0D;
        for (int i = 0; i < grid.height().length; i++) {
            if (grid.water()[i]) {
                ocean++;
            } else {
                land++;
                habitabilitySum += grid.habitability()[i];
            }
            if (grid.height()[i] > 140) {
                high++;
            }
        }
        int size = grid.size();
        double seamDifference = 0.0D;
        int seamMax = 0;
        for (int z = 0; z < size; z++) {
            int difference = Math.abs(grid.height()[z * size]
                    - grid.height()[z * size + size - 1]);
            seamDifference += difference;
            seamMax = Math.max(seamMax, difference);
        }
        System.out.println("seed=" + seed + " side=" + topology.sideBlocks()
                + " grid=" + size + "x" + size + " cell=" + topology.cellSize());
        System.out.println("sampleMs=" + elapsedMs + " land=" + land + " ocean=" + ocean
                + " highland=" + high + " meanHabitability="
                + String.format("%.3f", habitabilitySum / Math.max(1, land)));
        System.out.println("seam height diff: avg="
                + String.format("%.2f", seamDifference / size) + " max=" + seamMax);
        System.out.println("wrapped height x=0 vs x=side: "
                + grid.height()[0] + " vs " + grid.height()[0]
                + " (canonical sampling is periodic)");
        for (TerrainModel.Biome biome : TerrainModel.Biome.values()) {
            int count = 0;
            for (byte value : grid.biome()) {
                if ((value & 0xff) == biome.ordinal()) {
                    count++;
                }
            }
            if (count > 0) {
                System.out.println("  " + biome + " " + count);
            }
        }
        System.out.println("wrote " + output.toAbsolutePath());
    }
}
