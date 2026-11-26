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
        return compute(size, size, height, water, true);
    }

    static Result computeRegion(int width, int depth, short[] height, boolean[] water) {
        return compute(width, depth, height, water, false);
    }

    private static Result compute(int width, int depth, short[] height, boolean[] water,
                                  boolean wrap) {
        int count = width * depth;
        int[] filled = priorityFlood(width, depth, height, water, wrap);
        int[] receiver = new int[count];
        int[] flow = new int[count];
        Arrays.fill(receiver, -1);
        Arrays.fill(flow, 1);

        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                int index = z * width + x;
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
                        int ni = neighbor(x + dx, z + dz, width, depth, wrap);
                        if (ni < 0) {
                            continue;
                        }
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
        return new Result(river, lake, flow, buildPaths(width, river, receiver, flow));
    }

    private static int neighbor(int x, int z, int width, int depth, boolean wrap) {
        if (wrap) {
            return Math.floorMod(z, depth) * width + Math.floorMod(x, width);
        }
        return x < 0 || x >= width || z < 0 || z >= depth ? -1 : z * width + x;
    }

    private static int[] priorityFlood(int width, int depth, short[] height, boolean[] water,
                                       boolean wrap) {
        int count = width * depth;
        int[] filled = new int[count];
        boolean[] visited = new boolean[count];
        java.util.PriorityQueue<Integer> queue = new java.util.PriorityQueue<>(
                Comparator.comparingInt(i -> filled[i]));
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
        while (!queue.isEmpty()) {
            int current = queue.poll();
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
                if (next < 0 || !river[next] || usedEdge[current]) break;
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
                paths.add(new RiverPath(smooth(nodes), widthBlocks));
            }
        }
        return paths;
    }

    private static List<RiverPath.Node> smooth(List<RiverPath.Node> input) {
        List<RiverPath.Node> points = input;
        for (int pass = 0; pass < 2; pass++) {
            List<RiverPath.Node> next = new ArrayList<>();
            next.add(points.get(0));
            for (int i = 1; i < points.size(); i++) {
                RiverPath.Node a = points.get(i - 1);
                RiverPath.Node b = points.get(i);
                next.add(new RiverPath.Node(
                        (int) Math.round(a.x() * 0.75D + b.x() * 0.25D),
                        (int) Math.round(a.z() * 0.75D + b.z() * 0.25D)));
                next.add(new RiverPath.Node(
                        (int) Math.round(a.x() * 0.25D + b.x() * 0.75D),
                        (int) Math.round(a.z() * 0.25D + b.z() * 0.75D)));
            }
            next.add(points.get(points.size() - 1));
            points = next;
        }
        return List.copyOf(points);
    }
}
