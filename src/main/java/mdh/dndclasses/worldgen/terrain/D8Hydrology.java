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

    public record Result(boolean[] river, boolean[] lake, int[] flow, List<RiverPath> paths,
                         int[] receiver, int[] downstreamFirst, int[] lakeLevel) {
    }

    private D8Hydrology() {
    }

    public static Result compute(WorldTopology topology, short[] height, boolean[] water) {
        return compute(topology.gridSize(), height, water);
    }

    public static Result compute(int size, short[] height, boolean[] water) {
        return compute(size, size, height, water, null, true);
    }

    static Result computeRegion(int width, int depth, short[] height, boolean[] water, double[] wetness) {
        return compute(width, depth, height, water, wetness, false);
    }

    private static Result compute(int width, int depth, short[] height, boolean[] water,
                                  double[] wetness, boolean wrap) {
        int count = width * depth;
        int[] receiver = new int[count];
        int[] order = new int[count];
        Arrays.fill(receiver, -1);
        int[] filled = priorityFlood(width, depth, height, water, wrap, receiver, order);

        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                int index = z * width + x;
                if (water[index]) {
                    continue;
                }
                int best = receiver[index];
                int bestHeight = filled[index];
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dz == 0) {
                            continue;
                        }
                        int ni = neighbor(x + dx, z + dz, width, depth, wrap);
                        if (ni < 0) {
                            continue;
                        }
                        int nh = filled[ni];
                        if (nh < bestHeight) {
                            bestHeight = nh;
                            best = ni;
                        }
                    }
                }
                receiver[index] = best;
            }
        }

        int[] flow = accumulate(receiver, order, wetness);
        boolean[] river = new boolean[count];
        boolean[] lake = new boolean[count];
        int[] lakeLevel = new int[count];
        if (wetness != null) {
            var basin = new java.util.ArrayDeque<Integer>();
            for (int i : order) {
                if (lake[i] || water[i] || filled[i] < height[i] + 4
                        || wetness[i] < 0.52D || flow[i] < 52) continue;
                int spill = filled[i];
                lake[i] = true;
                basin.add(i);
                while (!basin.isEmpty()) {
                    int cell = basin.removeFirst();
                    lakeLevel[cell] = spill;
                    receiver[cell] = -1;
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx != 0 && dz != 0) continue;
                            int next = neighbor(cell % width + dx, cell / width + dz, width, depth, wrap);
                            if (next < 0 || lake[next] || water[next] || filled[next] != spill
                                    || height[next] >= spill) continue;
                            lake[next] = true;
                            basin.add(next);
                        }
                    }
                }
            }
            flow = accumulate(receiver, order, wetness);
        }
        for (int i = 0; i < count; i++) {
            river[i] = !water[i] && !lake[i] && flow[i] >= (wetness == null ? 24 : 36);
        }
        return new Result(river, lake, flow, buildPaths(width, river, receiver, flow),
                receiver, order, lakeLevel);
    }

    private static int[] accumulate(int[] receiver, int[] order, double[] wetness) {
        int[] flow = new int[order.length];
        for (int i = 0; i < flow.length; i++) {
            flow[i] = wetness == null ? 1 : (int) Math.round(4.0D * wetness[i]);
        }
        for (int i = order.length - 1; i >= 0; i--) {
            int cell = order[i];
            int next = receiver[cell];
            if (next >= 0) flow[next] += flow[cell];
        }
        return flow;
    }

    private static int neighbor(int x, int z, int width, int depth, boolean wrap) {
        if (wrap) {
            return Math.floorMod(z, depth) * width + Math.floorMod(x, width);
        }
        return x < 0 || x >= width || z < 0 || z >= depth ? -1 : z * width + x;
    }

    private static int[] priorityFlood(int width, int depth, short[] height, boolean[] water,
                                       boolean wrap, int[] receiver, int[] order) {
        int count = width * depth;
        int[] filled = new int[count];
        boolean[] visited = new boolean[count];
        java.util.PriorityQueue<Integer> queue = new java.util.PriorityQueue<>(
                Comparator.<Integer>comparingInt(i -> filled[i]).thenComparingInt(i -> i));
        for (int i = 0; i < count; i++) {
            filled[i] = height[i];
            if (water[i]) {
                filled[i] = 63;
            }
            int x = i % width;
            int z = i / width;
            if (water[i] || (!wrap && (x == 0 || x == width - 1 || z == 0 || z == depth - 1))) {
                visited[i] = true;
                queue.add(i);
            }
        }
        if (queue.isEmpty()) {
            filled[0] = height[0];
            visited[0] = true;
            queue.add(0);
        }
        int rank = 0;
        while (!queue.isEmpty()) {
            int current = queue.poll();
            order[rank++] = current;
            int x = current % width;
            int z = current / width;
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    int index = neighbor(x + dx, z + dz, width, depth, wrap);
                    if (index < 0 || visited[index]) {
                        continue;
                    }
                    visited[index] = true;
                    receiver[index] = current;
                    filled[index] = Math.max(height[index], filled[current]);
                    queue.add(index);
                }
            }
        }
        return filled;
    }

    private static List<RiverPath> buildPaths(int width, boolean[] river, int[] receiver, int[] flow) {
        int count = river.length;
        int[] upstream = new int[count];
        for (int i = 0; i < count; i++) {
            if (river[i] && receiver[i] >= 0 && river[receiver[i]]) upstream[receiver[i]]++;
        }
        boolean[] usedEdge = new boolean[count];
        List<RiverPath> paths = new ArrayList<>();
        for (int start = 0; start < count; start++) {
            if (!river[start] || upstream[start] == 1) {
                continue;
            }
            List<RiverPath.Node> nodes = new ArrayList<>();
            int current = start;
            int maxFlow = 0;
            while (current >= 0 && river[current]) {
                nodes.add(new RiverPath.Node(current % width, current / width));
                maxFlow = Math.max(maxFlow, flow[current]);
                int next = receiver[current];
                if (next < 0 || usedEdge[current]) break;
                if (!river[next]) {
                    nodes.add(new RiverPath.Node(next % width, next / width));
                    break;
                }
                usedEdge[current] = true;
                current = next;
                if (upstream[current] != 1) {
                    nodes.add(new RiverPath.Node(current % width, current / width));
                    maxFlow = Math.max(maxFlow, flow[current]);
                    break;
                }
            }
            if (nodes.size() >= 2) {
                int widthBlocks = 4 + (int) Math.min(236.0D, Math.sqrt(maxFlow) * 2.2D);
                paths.add(new RiverPath(List.copyOf(nodes), widthBlocks));
            }
        }
        return paths;
    }

}
