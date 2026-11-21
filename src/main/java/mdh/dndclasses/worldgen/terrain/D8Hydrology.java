package mdh.dndclasses.worldgen.terrain;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Coarse D8 drainage and flow accumulation. Clean-room implementation; it is used as the shared
 * hydrology layer for the map and, later, for in-game river carving.
 */
public final class D8Hydrology {

    public record Result(boolean[] river, boolean[] lake, int[] flow) {
    }

    private D8Hydrology() {
    }

    public static Result compute(WorldTopology topology, short[] height, boolean[] water) {
        int size = topology.gridSize();
        int count = size * size;
        int[] receiver = new int[count];
        int[] flow = new int[count];
        Arrays.fill(receiver, -1);
        Arrays.fill(flow, 1);

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int index = z * size + x;
                if (water[index]) {
                    continue;
                }
                int best = -1;
                int bestHeight = height[index];
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dz == 0) {
                            continue;
                        }
                        int nx = Math.floorMod(x + dx, size);
                        int nz = Math.floorMod(z + dz, size);
                        int ni = nz * size + nx;
                        int nh = water[ni] ? 63 : height[ni];
                        if (nh < bestHeight) {
                            bestHeight = nh;
                            best = ni;
                        }
                    }
                }
                receiver[index] = best;
            }
        }

        Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingInt(i -> -height[i]));

        for (int index : order) {
            int next = receiver[index];
            if (next >= 0 && !water[index]) {
                flow[next] += flow[index];
            }
        }

        boolean[] river = new boolean[count];
        boolean[] lake = new boolean[count];
        for (int i = 0; i < count; i++) {
            if (water[i]) {
                continue;
            }
            if (flow[i] >= 70) {
                river[i] = true;
            }
            if (receiver[i] < 0 && height[i] > 63) {
                lake[i] = true;
            }
        }
        return new Result(river, lake, flow);
    }
}
