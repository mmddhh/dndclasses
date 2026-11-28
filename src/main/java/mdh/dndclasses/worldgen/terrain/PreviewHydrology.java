package mdh.dndclasses.worldgen.terrain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/** Builds the final preview network. Intermediate erosion uses its own unfiltered runoff. */
final class PreviewHydrology {
    static final byte DRY = 1, SEASONAL = 2, OPEN_LAKE = 3, CLOSED_LAKE = 4;
    static final double TRIBUTARY_FLOW = 8, MAIN_FLOW = 32;

    record Result(int[] receiver, double[] discharge, byte[] surface, boolean[] channel,
                  boolean[] outlet, boolean[] inland, int lakeCount, int outletCount) {}

    private record Basin(ArrayList<Integer> cells, int anchor) {}

    static Result build(int side, int resolution, double[] height, boolean[] sea, double[] spill,
                        int[] order, int[] rawReceiver, double[] rain, double[] rawFlow,
                        double[] evaporation) {
        int size = height.length;
        int[] rank = new int[size], basinId = new int[size];
        Arrays.fill(basinId, -1);
        for (int i = 0; i < size; i++) rank[order[i]] = i;
        var basins = new ArrayList<Basin>();
        var queue = new ArrayDeque<Integer>();
        // Priority-flood order ensures the first cell of each depression is its downstream anchor.
        for (int i : order) {
            if (basinId[i] >= 0 || sea[i] || spill[i] - height[i] < 0.05) continue;
            int id = basins.size();
            var members = new ArrayList<Integer>();
            basins.add(new Basin(members, i));
            basinId[i] = id;
            queue.add(i);
            while (!queue.isEmpty()) {
                int j = queue.remove();
                members.add(j);
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int k = neighbor(side, j, dx, dz);
                        if (basinId[k] < 0 && !sea[k] && spill[k] == spill[i]
                                && spill[k] - height[k] >= 0.05) {
                            basinId[k] = id;
                            queue.add(k);
                        }
                    }
                }
            }
        }

        int[] receiver = rawReceiver.clone();
        double[] valley = new double[size];
        for (int i : order) {
            if (sea[i] || basinId[i] >= 0) continue;
            int best = receiver[i];
            double bestScore = 0;
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dz == 0) continue;
                    int j = neighbor(side, i, dx, dz);
                    if (rank[j] >= rank[i] || spill[j] > spill[i]) continue;
                    double descent = height[i] - (sea[j] ? 0 : height[j]);
                    if (descent <= 0) continue;
                    double grade = descent / (dx != 0 && dz != 0 ? Math.sqrt(2) : 1);
                    double trough = crossValley(side, j, dx, dz, resolution, height, sea);
                    // Give convergent routes a preference on gentle terrain; still require descent.
                    double score = grade * (1 + 0.40 * Math.min(2.0, trough))
                            + 0.11 * Math.log1p(rawFlow[j]);
                    if (score > bestScore) {
                        bestScore = score;
                        best = j;
                    }
                }
            }
            receiver[i] = best;
            int dx = delta(side, i % side, best % side), dz = delta(side, i / side, best / side);
            valley[i] = crossValley(side, i, dx, dz, resolution, height, sea);
        }

        double[] flow = rain.clone();
        double[] largestBranch = new double[size];
        double[] secondBranch = new double[size];
        byte[] surface = new byte[size];
        boolean[] outlet = new boolean[size];
        int lakeCount = 0, outletCount = 0;
        for (int k = size - 1; k >= 0; k--) {
            int i = order[k], j = receiver[i], id = basinId[i];
            if (j < 0) continue;
            if (id >= 0) {
                Basin basin = basins.get(id);
                if (i != basin.anchor) {
                    flow[basin.anchor] += flow[i];
                    continue;
                }
                double demand = 0;
                for (int cell : basin.cells) demand += evaporation[cell];
                double supply = flow[i];
                if (supply >= demand) {
                    for (int cell : basin.cells) surface[cell] = OPEN_LAKE;
                    lakeCount++;
                    flow[i] = supply - demand;
                    outlet[i] = flow[i] > 1e-9;
                    if (outlet[i]) outletCount++;
                } else {
                    // Fill the lowest cells first until the evaporative budget is exhausted.
                    basin.cells.sort(Comparator.<Integer>comparingDouble(cell -> height[cell])
                            .thenComparingInt(cell -> cell));
                    byte wet = supply >= demand * 0.68 ? CLOSED_LAKE : SEASONAL;
                    double remaining = supply;
                    boolean hasWater = false;
                    for (int start = 0; start < basin.cells.size();) {
                        double level = height[basin.cells.get(start)];
                        int end = start;
                        double cost = 0;
                        do {
                            cost += evaporation[basin.cells.get(end++)];
                        } while (end < basin.cells.size() && height[basin.cells.get(end)] == level);
                        if (remaining < cost) break;
                        for (int n = start; n < end; n++) surface[basin.cells.get(n)] = wet;
                        remaining -= cost;
                        hasWater = true;
                        start = end;
                    }
                    if (hasWater) lakeCount++;
                    else for (int cell : basin.cells) surface[cell] = DRY;
                    flow[i] = 0;
                    continue;
                }
            }
            double discharge = flow[i];
            flow[j] += discharge;
            if (discharge > largestBranch[j]) {
                secondBranch[j] = largestBranch[j];
                largestBranch[j] = discharge;
            } else if (discharge > secondBranch[j]) {
                secondBranch[j] = discharge;
            }
        }

        int[] landArea = landAreas(side, sea);
        int[] lengthToWater = new int[size];
        boolean[] channel = new boolean[size], inland = new boolean[size];
        for (int i : order) {
            int j = receiver[i];
            if (j < 0) continue;
            if (basinId[i] >= 0 && !outlet[i]) {
                inland[i] = true;
            } else {
                inland[i] = inland[j];
                lengthToWater[i] = lengthToWater[j] + 1;
            }
        }
        for (int k = size - 1; k >= 0; k--) {
            int i = order[k], j = receiver[i];
            if (j < 0 || basinId[i] >= 0 && !outlet[i]) continue;
            boolean smallIsland = landArea[i] <= 120 * resolution * resolution;
            double minimum = smallIsland ? 4.5 : TRIBUTARY_FLOW;
            boolean convergence = secondBranch[i] >= Math.max(0.85, flow[i] * 0.12);
            boolean valleyHead = valley[i] >= 0.18 && (convergence || valley[i] >= 0.65);
            boolean majorHead = flow[i] >= MAIN_FLOW && (valley[i] >= 0.08 || convergence);
            boolean head = flow[i] >= minimum && lengthToWater[i] >= 2 * resolution
                    && (valleyHead || majorHead || smallIsland && convergence);
            channel[i] |= outlet[i] || head;
            if (channel[i] && basinId[j] < 0 && !sea[j]) channel[j] = true;
        }
        // A partially filled basin has a dry apron between its lip and the current shore.
        // Route visible inlets across that apron without creating an outlet.
        int[] toShore = new int[size];
        Arrays.fill(toShore, -1);
        for (int i : order) {
            if (surface[i] == SEASONAL || surface[i] == CLOSED_LAKE) {
                toShore[i] = i;
                queue.add(i);
            }
        }
        while (!queue.isEmpty()) {
            int i = queue.remove();
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int j = neighbor(side, i, dx, dz);
                    if (toShore[j] < 0 && basinId[j] == basinId[i]) {
                        toShore[j] = i;
                        queue.add(j);
                    }
                }
            }
        }
        for (int i : order) {
            if (!channel[i] || basinId[i] >= 0) continue;
            int j = receiver[i];
            while (surface[j] == 0 && toShore[j] >= 0 && !channel[j]) {
                channel[j] = true;
                inland[j] = true;
                flow[j] = Math.max(flow[j], flow[i]);
                receiver[j] = toShore[j];
                j = receiver[j];
            }
        }
        return new Result(receiver, flow, surface, channel, outlet, inland, lakeCount, outletCount);
    }

    private static double crossValley(int side, int i, int dx, int dz, int radius,
                                      double[] height, boolean[] sea) {
        if (sea[i]) return 0;
        int left = neighbor(side, i, -dz * radius, dx * radius);
        int right = neighbor(side, i, dz * radius, -dx * radius);
        if (sea[left] || sea[right]) return 0;
        return Math.max(0, Math.min(height[left], height[right]) - height[i]);
    }

    private static int[] landAreas(int side, boolean[] sea) {
        int[] area = new int[sea.length];
        int[] members = new int[sea.length];
        for (int first = 0; first < sea.length; first++) {
            if (sea[first] || area[first] != 0) continue;
            int count = 1;
            members[0] = first;
            area[first] = -1;
            for (int n = 0; n < count; n++) {
                int i = members[n];
                for (int d = -1; d <= 1; d += 2) {
                    for (int axis = 0; axis < 2; axis++) {
                        int j = neighbor(side, i, axis == 0 ? d : 0, axis == 1 ? d : 0);
                        if (sea[j] || area[j] != 0) continue;
                        area[j] = -1;
                        members[count++] = j;
                    }
                }
            }
            for (int n = 0; n < count; n++) area[members[n]] = count;
        }
        return area;
    }

    private static int delta(int side, int a, int b) {
        return Math.floorMod(b - a + 1, side) - 1;
    }

    private static int neighbor(int side, int i, int dx, int dz) {
        return Math.floorMod(i / side + dz, side) * side + Math.floorMod(i % side + dx, side);
    }
}
