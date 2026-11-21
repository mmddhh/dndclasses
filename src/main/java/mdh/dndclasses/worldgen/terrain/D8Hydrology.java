package mdh.dndclasses.worldgen.terrain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Coarse D8 drainage and flow accumulation. Clean-room implementation; it is used as the shared
 * hydrology layer for the map and, later, for in-game river carving.
 */
public final class D8Hydrology {

    public record Result(boolean[] river, boolean[] lake, int[] flow, List<RiverPath> paths) {
    }

    private D8Hydrology() {
    }

    public static Result compute(WorldTopology topology, short[] height, boolean[] water) {
        return compute(topology.gridSize(), height, water);
    }

    public static Result compute(int size, short[] height, boolean[] water) {
        int count = size * size;
        int[] filled = priorityFlood(size, height, water);
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
                int bestHeight = filled[index];
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dz == 0) {
                            continue;
                        }
                        int nx = Math.floorMod(x + dx, size);
                        int nz = Math.floorMod(z + dz, size);
                        int ni = nz * size + nx;
                        int nh = water[ni] ? 63 : filled[ni];
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
        Arrays.sort(order, Comparator.comparingInt(i -> -filled[i]));

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
            if (flow[i] >= 24) {
                river[i] = true;
            }
            if (receiver[i] < 0 && height[i] > 63 && flow[i] < 2) {
                lake[i] = true;
            }
        }
        return new Result(river, lake, flow, buildPaths(size, river, receiver, flow));
    }

    private static int[] priorityFlood(int size, short[] height, boolean[] water) {
        int count = size * size;
        int[] filled = new int[count];
        boolean[] visited = new boolean[count];
        java.util.PriorityQueue<Integer> queue = new java.util.PriorityQueue<>(
                Comparator.comparingInt(i -> filled[i]));
        for (int i = 0; i < count; i++) {
            filled[i] = height[i];
            if (water[i]) {
                filled[i] = 63;
                visited[i] = true;
                queue.add(i);
            }
        }
        if (queue.isEmpty()) {
            filled[0] = height[0];
            visited[0] = true;
            queue.add(0);
        }
        while (!queue.isEmpty()) {
            int current = queue.poll();
            int x = current % size;
            int z = current / size;
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    int nx = Math.floorMod(x + dx, size);
                    int nz = Math.floorMod(z + dz, size);
                    int index = nz * size + nx;
                    if (visited[index]) {
                        continue;
                    }
                    visited[index] = true;
                    filled[index] = Math.max(height[index], filled[current]);
                    queue.add(index);
                }
            }
        }
        return filled;
    }

    private static List<RiverPath> buildPaths(int size, boolean[] river, int[] receiver, int[] flow) {
        int count = size * size;
        boolean[] visited = new boolean[count];
        List<RiverPath> paths = new ArrayList<>();
        for (int start = 0; start < count; start++) {
            if (!river[start] || visited[start]) {
                continue;
            }
            List<RiverPath.Node> nodes = new ArrayList<>();
            int current = start;
            int maxFlow = 0;
            while (current >= 0 && river[current] && !visited[current]) {
                visited[current] = true;
                nodes.add(new RiverPath.Node(current % size, current / size));
                maxFlow = Math.max(maxFlow, flow[current]);
                current = receiver[current];
            }
            if (nodes.size() >= 2) {
                int width = 1 + Math.min(7, maxFlow / 45);
                paths.add(new RiverPath(List.copyOf(nodes), width));
            }
        }
        return paths;
    }
}
