package mdh.dndclasses.worldgen.terrain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Coarse river network on the torus:
 * <ol>
 *   <li>priority-flood the height grid so depressions have a spill level;</li>
 *   <li>route every land cell to its lowest filled neighbour;</li>
 *   <li>accumulate upstream area and keep cells above the river threshold;</li>
 *   <li>trace those cells downhill to the sea.</li>
 * </ol>
 * The result is a diagnostic grid; the in-game generator can refine each river cell later.
 */
public final class RiverNetwork {

    public record Result(boolean[] river, int[] flow, List<RiverPath> paths) {
    }

    private RiverNetwork() {
    }

    public static Result compute(WorldTopology topology, short[] height, boolean[] water,
                                 float[] humidity) {
        int size = topology.gridSize();
        int count = size * size;
        int[] filled = new int[count];
        boolean[] visited = new boolean[count];
        PriorityQueue<int[]> open = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));

        for (int i = 0; i < count; i++) {
            filled[i] = height[i];
            if (water[i]) {
                visited[i] = true;
                open.add(new int[]{height[i], i});
            }
        }
        while (!open.isEmpty()) {
            int[] cell = open.poll();
            int index = cell[1];
            int gx = index % size;
            int gz = index / size;
            for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}}) {
                int nx = Math.floorMod(gx + dir[0], size);
                int nz = Math.floorMod(gz + dir[1], size);
                int next = nz * size + nx;
                if (visited[next]) {
                    continue;
                }
                visited[next] = true;
                filled[next] = Math.max(height[next], filled[index]);
                open.add(new int[]{filled[next], next});
            }
        }

        int[] waterDistance = new int[count];
        java.util.Arrays.fill(waterDistance, -1);
        java.util.ArrayDeque<Integer> waterQueue = new java.util.ArrayDeque<>();
        for (int i = 0; i < count; i++) {
            if (water[i]) {
                waterDistance[i] = 0;
                waterQueue.add(i);
            }
        }
        while (!waterQueue.isEmpty()) {
            int current = waterQueue.poll();
            int gx = current % size;
            int gz = current / size;
            for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}}) {
                int nx = Math.floorMod(gx + dir[0], size);
                int nz = Math.floorMod(gz + dir[1], size);
                int next = nz * size + nx;
                if (waterDistance[next] == -1) {
                    waterDistance[next] = waterDistance[current] + 1;
                    waterQueue.add(next);
                }
            }
        }

        int[] downstream = new int[count];
        java.util.Arrays.fill(downstream, -1);
        for (int index = 0; index < count; index++) {
            if (water[index]) {
                continue;
            }
            int gx = index % size;
            int gz = index / size;
            int best = -1;
            int bestHeight = filled[index];
            for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}}) {
                int nx = Math.floorMod(gx + dir[0], size);
                int nz = Math.floorMod(gz + dir[1], size);
                int next = nz * size + nx;
                if (filled[next] < bestHeight) {
                    bestHeight = filled[next];
                    best = next;
                }
            }
            downstream[index] = best;
        }

        Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++) {
            order[i] = i;
        }
        java.util.Arrays.sort(order, Comparator.comparingInt((Integer i) -> -filled[i]));
        int[] flow = new int[count];
        java.util.Arrays.fill(flow, 1);
        for (int index : order) {
            int next = downstream[index];
            if (next >= 0) {
                flow[next] += flow[index];
            }
        }

        boolean[] river = new boolean[count];
        List<RiverPath> paths = new ArrayList<>();
        List<Integer> candidates = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            if (!water[index] && height[index] > TerrainModel.SEA_LEVEL + 45) {
                candidates.add(index);
            }
        }
        candidates.sort(Comparator.comparingDouble((Integer index) ->
                -(height[index] + humidity[index] * 120.0D)));
        List<Integer> sources = new ArrayList<>();
        // Wet quadrants get more sources; dry quadrants still get a baseline.
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            double wetness = 0.0D;
            int cells = 0;
            for (int index = 0; index < count; index++) {
                if (quadrantOf(index, size) == quadrant) {
                    wetness += humidity[index];
                    cells++;
                }
            }
            int maxInQuadrant = 4 + (int) Math.round((wetness / Math.max(1, cells)) * 9.0D);
            int selectedInQuadrant = 0;
            for (int candidate : candidates) {
                if (quadrantOf(candidate, size) != quadrant) {
                    continue;
                }
                if (isFarEnough(candidate, sources, size, 5)) {
                    sources.add(candidate);
                    selectedInQuadrant++;
                    if (selectedInQuadrant >= maxInQuadrant) {
                        break;
                    }
                }
            }
        }
        // Fill any remaining slots globally.
        for (int candidate : candidates) {
            if (sources.size() >= 40) {
                break;
            }
            if (isFarEnough(candidate, sources, size, 6)) {
                sources.add(candidate);
            }
        }

        for (int source : sources) {
            List<Integer> path = new ArrayList<>();
            boolean[] pathVisited = new boolean[count];
            int cursor = source;
            int guard = 0;
            while (cursor >= 0 && guard++ < count) {
                path.add(cursor);
                pathVisited[cursor] = true;
                if (water[cursor]) {
                    break;
                }
                int next = downstream[cursor];
                if (next < 0 || next == cursor || pathVisited[next]) {
                    int gx = cursor % size;
                    int gz = cursor / size;
                    int fallback = -1;
                    int bestDistance = Integer.MAX_VALUE;
                    for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}}) {
                        int nx = Math.floorMod(gx + dir[0], size);
                        int nz = Math.floorMod(gz + dir[1], size);
                        int candidate = nz * size + nx;
                        if (!pathVisited[candidate] && waterDistance[candidate] >= 0
                                && waterDistance[candidate] < bestDistance) {
                            bestDistance = waterDistance[candidate];
                            fallback = candidate;
                        }
                    }
                    next = fallback;
                }
                if (next < 0) {
                    break;
                }
                cursor = next;
            }
            if (path.size() >= 8 && water[path.get(path.size() - 1)]) {
                RiverPath smoothed = smooth(path, size);
                paths.add(smoothed);
                for (RiverPath.Node node : smoothed.points()) {
                    river[node.z() * size + node.x()] = true;
                }
                for (int index : path) {
                    flow[index] += 100;
                }
            }
        }

        // Small tributaries where upstream area is genuinely large.
        for (int index = 0; index < count; index++) {
            if (water[index] || height[index] <= TerrainModel.SEA_LEVEL + 1) {
                continue;
            }
            if (flow[index] >= 240) {
                List<Integer> path = new ArrayList<>();
                int cursor = index;
                int guard = 0;
                while (cursor >= 0 && guard++ < count) {
                    path.add(cursor);
                    if (water[cursor]) {
                        break;
                    }
                    cursor = downstream[cursor];
                }
                if (path.size() >= 8 && water[path.get(path.size() - 1)]) {
                    RiverPath smoothed = smooth(path, size);
                    paths.add(smoothed);
                    for (RiverPath.Node node : smoothed.points()) {
                        river[node.z() * size + node.x()] = true;
                    }
                }
            }
        }
        return new Result(river, flow, paths);
    }

    private static RiverPath smooth(List<Integer> path, int size) {
        int[] xs = new int[path.size()];
        int[] zs = new int[path.size()];
        int half = size / 2;
        for (int i = 0; i < path.size(); i++) {
            int rawX = path.get(i) % size;
            int rawZ = path.get(i) / size;
            int x = rawX;
            int z = rawZ;
            if (i > 0) {
                while (x - xs[i - 1] > half) {
                    x -= size;
                }
                while (xs[i - 1] - x > half) {
                    x += size;
                }
                while (z - zs[i - 1] > half) {
                    z -= size;
                }
                while (zs[i - 1] - z > half) {
                    z += size;
                }
            }
            xs[i] = x;
            zs[i] = z;
        }

        List<RiverPath.Node> points = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            int a = Math.max(0, i - 1);
            int b = i;
            int c = i + 1;
            int d = Math.min(path.size() - 1, i + 2);
            for (int step = 0; step < 4; step++) {
                double t = step / 4.0D;
                int x = Math.floorMod((int) Math.round(
                        catmull(xs[a], xs[b], xs[c], xs[d], t)), size);
                int z = Math.floorMod((int) Math.round(
                        catmull(zs[a], zs[b], zs[c], zs[d], t)), size);
                points.add(new RiverPath.Node(x, z));
            }
        }
        int last = path.size() - 1;
        points.add(new RiverPath.Node(Math.floorMod(xs[last], size),
                Math.floorMod(zs[last], size)));
        return new RiverPath(List.copyOf(points));
    }

    private static double catmull(double a, double b, double c, double d, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5D * ((2.0D * b)
                + (-a + c) * t
                + (2.0D * a - 5.0D * b + 4.0D * c - d) * t2
                + (-a + 3.0D * b - 3.0D * c + d) * t3);
    }

    private static int quadrantOf(int index, int size) {
        int gx = index % size;
        int gz = index / size;
        return (gx < size / 2 ? 0 : 1) + (gz < size / 2 ? 0 : 2);
    }

    private static boolean isFarEnough(int candidate, List<Integer> sources, int size, int spacing) {
        int gx = candidate % size;
        int gz = candidate / size;
        for (int source : sources) {
            int sx = source % size;
            int sz = source / size;
            int dx = Math.abs(gx - sx);
            int dz = Math.abs(gz - sz);
            dx = Math.min(dx, size - dx);
            dz = Math.min(dz, size - dz);
            if (Math.max(dx, dz) < spacing) {
                return false;
            }
        }
        return true;
    }
}
