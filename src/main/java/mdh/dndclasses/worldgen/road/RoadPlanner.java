package mdh.dndclasses.worldgen.road;

import mdh.dndclasses.worldgen.WorldPlan;
import mdh.dndclasses.worldgen.road.RoadNetwork.Road;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Pure road planner:
 * <ul>
 *   <li>internal nation MST (capital + towns);</li>
 *   <li>capital-to-capital MST (highways);</li>
 *   <li>A* paths over a coarse terrain sample;</li>
 *   <li>cross-nation roads get a provisional border-post marker.</li>
 * </ul>
 * It deliberately does not connect every pair of settlements.
 */
public final class RoadPlanner {

    private static final int STEP = 32;
    private static final int MAX_EXPANDED = 200_000;

    private RoadPlanner() {
    }

    public static RoadNetwork plan(WorldPlan plan, TerrainCost terrain) {
        Map<Integer, WorldPlan.Settlement> capitals = new HashMap<>();
        for (WorldPlan.Settlement settlement : plan.settlements()) {
            capitals.putIfAbsent(settlement.nationId(), settlement);
        }

        Set<String> usedEdges = new HashSet<>();
        List<Road> roads = new ArrayList<>();
        List<RoadNetwork.FailedRoute> failed = new ArrayList<>();
        Map<Integer, List<WorldPlan.Settlement>> components = landComponents(plan, terrain);

        // Internal roads only inside one connected landmass. A settlement on another island is
        // deliberately left without a land road (a ferry/sea route is a separate later feature).
        for (WorldPlan.Nation nation : plan.nations()) {
            for (List<WorldPlan.Settlement> component : components.values()) {
                List<WorldPlan.Settlement> settlements = component.stream()
                        .filter(settlement -> settlement.nationId() == nation.id())
                        .sorted(Comparator.comparingInt(settlement ->
                                settlement.id() == capitals.get(nation.id()).id() ? 0 : 1))
                        .toList();
                for (int[] edge : mst(settlements)) {
                    addRoad(roads, usedEdges, failed, plan, terrain,
                            settlements.get(edge[0]), settlements.get(edge[1]), false);
                }
            }
        }

        // Cross-border roads: only the nearest pair per nation pair *within the same landmass*.
        for (List<WorldPlan.Settlement> component : components.values()) {
            Set<Integer> nations = new HashSet<>();
            for (WorldPlan.Settlement settlement : component) {
                nations.add(settlement.nationId());
            }
            List<Integer> nationIds = new ArrayList<>(nations);
            for (int a = 0; a < nationIds.size(); a++) {
                for (int b = a + 1; b < nationIds.size(); b++) {
                    WorldPlan.Settlement bestA = null;
                    WorldPlan.Settlement bestB = null;
                    double bestDistance = Double.MAX_VALUE;
                    for (WorldPlan.Settlement sa : component) {
                        if (sa.nationId() != nationIds.get(a)) {
                            continue;
                        }
                        for (WorldPlan.Settlement sb : component) {
                            if (sb.nationId() != nationIds.get(b)) {
                                continue;
                            }
                            double dx = sa.x() - sb.x();
                            double dz = sa.z() - sb.z();
                            double distance = dx * dx + dz * dz;
                            if (distance < bestDistance) {
                                bestDistance = distance;
                                bestA = sa;
                                bestB = sb;
                            }
                        }
                    }
                    if (bestA != null && bestDistance <= 2048.0D * 2048.0D) {
                        addRoad(roads, usedEdges, failed, plan, terrain, bestA, bestB, true);
                    }
                }
            }
        }

        List<RoadNetwork.BorderPost> posts = new ArrayList<>();
        for (Road road : roads) {
            if (road.nationA() != road.nationB() && !road.path().isEmpty()) {
                RoadNetwork.Node mid = road.path().get(road.path().size() / 2);
                posts.add(new RoadNetwork.BorderPost(mid.x(), mid.z(), road.nationA(),
                        road.nationB()));
            }
        }
        return new RoadNetwork(List.copyOf(roads), List.copyOf(posts), List.copyOf(failed));
    }

    private static void addRoad(List<Road> roads, Set<String> usedEdges,
                                List<RoadNetwork.FailedRoute> failed, WorldPlan plan,
                                TerrainCost terrain, WorldPlan.Settlement a,
                                WorldPlan.Settlement b, boolean crossNation) {
        String key = Math.min(a.id(), b.id()) + ":" + Math.max(a.id(), b.id());
        if (!usedEdges.add(key)) {
            return;
        }
        List<RoadNetwork.Node> path = aStar(terrain, a.x(), a.z(), b.x(), b.z());
        if (path.size() < 2) {
            failed.add(new RoadNetwork.FailedRoute(a.nationId(), b.nationId(),
                    a.x(), a.z(), b.x(), b.z(), "no_path_over_terrain"));
            return;
        }
        boolean capitalA = a.id() == firstSettlement(plan, a.nationId());
        boolean capitalB = b.id() == firstSettlement(plan, b.nationId());
        RoadNetwork.Tier tier;
        if (crossNation) {
            tier = RoadNetwork.Tier.ROAD;
        } else if (capitalA || capitalB) {
            tier = RoadNetwork.Tier.HIGHWAY;
        } else {
            tier = RoadNetwork.Tier.PATH;
        }
        String surface = switch (tier) {
            case HIGHWAY -> "stone_bricks";
            case ROAD -> "cobblestone";
            case PATH -> "dirt_path";
        };
        roads.add(new RoadNetwork.Road(roads.size(), tier, a.nationId(), b.nationId(),
                List.copyOf(path), surface));
    }

    private static int firstSettlement(WorldPlan plan, int nationId) {
        return plan.settlements().stream().filter(settlement -> settlement.nationId() == nationId)
                .mapToInt(WorldPlan.Settlement::id).findFirst().orElse(-1);
    }

    private static Map<Integer, List<WorldPlan.Settlement>> landComponents(
            WorldPlan plan, TerrainCost terrain) {
        if (plan.settlements().isEmpty()) {
            return Map.of();
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (WorldPlan.Settlement settlement : plan.settlements()) {
            minX = Math.min(minX, settlement.x());
            maxX = Math.max(maxX, settlement.x());
            minZ = Math.min(minZ, settlement.z());
            maxZ = Math.max(maxZ, settlement.z());
        }
        minX -= 256;
        maxX += 256;
        minZ -= 256;
        maxZ += 256;
        int cols = Math.max(1, (maxX - minX) / STEP + 1);
        int rows = Math.max(1, (maxZ - minZ) / STEP + 1);
        int[][] component = new int[rows][cols];
        for (int[] row : component) {
            java.util.Arrays.fill(row, -1);
        }
        int next = 0;
        for (int gz = 0; gz < rows; gz++) {
            for (int gx = 0; gx < cols; gx++) {
                if (component[gz][gx] != -1) {
                    continue;
                }
                int x = minX + gx * STEP;
                int z = minZ + gz * STEP;
                if (!terrain.passable(x, z)) {
                    continue;
                }
                int id = next++;
                ArrayDeque<int[]> queue = new ArrayDeque<>();
                queue.add(new int[]{gx, gz});
                component[gz][gx] = id;
                while (!queue.isEmpty()) {
                    int[] cell = queue.poll();
                    for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        int nx = cell[0] + dir[0];
                        int nz = cell[1] + dir[1];
                        if (nx < 0 || nz < 0 || nx >= cols || nz >= rows
                                || component[nz][nx] != -1) {
                            continue;
                        }
                        if (!terrain.passable(minX + nx * STEP, minZ + nz * STEP)) {
                            continue;
                        }
                        component[nz][nx] = id;
                        queue.add(new int[]{nx, nz});
                    }
                }
            }
        }
        Map<Integer, List<WorldPlan.Settlement>> result = new HashMap<>();
        for (WorldPlan.Settlement settlement : plan.settlements()) {
            int gx = (settlement.x() - minX) / STEP;
            int gz = (settlement.z() - minZ) / STEP;
            int id = -1;
            if (gx >= 0 && gz >= 0 && gx < cols && gz < rows) {
                id = component[gz][gx];
            }
            if (id < 0) {
                outer:
                for (int radius = 1; radius <= 8; radius++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            int nx = gx + dx;
                            int nz = gz + dz;
                            if (nx >= 0 && nz >= 0 && nx < cols && nz < rows
                                    && component[nz][nx] >= 0) {
                                id = component[nz][nx];
                                break outer;
                            }
                        }
                    }
                }
            }
            if (id >= 0) {
                result.computeIfAbsent(id, ignored -> new ArrayList<>()).add(settlement);
            }
        }
        return result;
    }

    /** Prim MST over a small settlement list; returns index pairs. */
    private static List<int[]> mst(List<WorldPlan.Settlement> settlements) {
        List<int[]> edges = new ArrayList<>();
        if (settlements.size() < 2) {
            return edges;
        }
        Set<Integer> connected = new HashSet<>();
        connected.add(0);
        while (connected.size() < settlements.size()) {
            int bestA = -1;
            int bestB = -1;
            double bestDistance = Double.MAX_VALUE;
            for (int a : connected) {
                for (int b = 0; b < settlements.size(); b++) {
                    if (connected.contains(b)) {
                        continue;
                    }
                    double dx = settlements.get(a).x() - settlements.get(b).x();
                    double dz = settlements.get(a).z() - settlements.get(b).z();
                    double distance = dx * dx + dz * dz;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestA = a;
                        bestB = b;
                    }
                }
            }
            if (bestB < 0) {
                break;
            }
            connected.add(bestB);
            edges.add(new int[]{bestA, bestB});
        }
        return edges;
    }

    private static List<RoadNetwork.Node> aStar(TerrainCost terrain, int startX, int startZ,
                                                int goalX, int goalZ) {
        int alignedStartX = Math.round(startX / (float) STEP) * STEP;
        int alignedStartZ = Math.round(startZ / (float) STEP) * STEP;
        int alignedGoalX = Math.round(goalX / (float) STEP) * STEP;
        int alignedGoalZ = Math.round(goalZ / (float) STEP) * STEP;
        int minX = Math.min(alignedStartX, alignedGoalX) - 512;
        int maxX = Math.max(alignedStartX, alignedGoalX) + 512;
        int minZ = Math.min(alignedStartZ, alignedGoalZ) - 512;
        int maxZ = Math.max(alignedStartZ, alignedGoalZ) + 512;
        long startKey = key(alignedStartX, alignedStartZ);
        long goalKey = key(alignedGoalX, alignedGoalZ);
        Map<Long, Double> gScore = new HashMap<>();
        Map<Long, Long> cameFrom = new HashMap<>();
        PriorityQueue<long[]> open = new PriorityQueue<>(Comparator.comparingDouble(a ->
                Double.longBitsToDouble(a[1])));
        gScore.put(startKey, 0.0D);
        open.add(new long[]{startKey,
                Double.doubleToLongBits(heuristic(alignedStartX, alignedStartZ,
                        alignedGoalX, alignedGoalZ))});
        int expanded = 0;
        while (!open.isEmpty() && expanded++ < MAX_EXPANDED) {
            long current = open.poll()[0];
            int cx = (int) (current >> 32);
            int cz = (int) current;
            if (current == goalKey) {
                List<RoadNetwork.Node> path = reconstruct(cameFrom, current);
                if (path.isEmpty() || path.get(0).x() != startX || path.get(0).z() != startZ) {
                    path = new ArrayList<>(path);
                    path.add(0, new RoadNetwork.Node(startX, startZ));
                }
                if (path.get(path.size() - 1).x() != goalX
                        || path.get(path.size() - 1).z() != goalZ) {
                    path.add(new RoadNetwork.Node(goalX, goalZ));
                }
                return path;
            }
            double currentG = gScore.getOrDefault(current, Double.MAX_VALUE);
            for (int[] dir : new int[][]{{STEP, 0}, {-STEP, 0}, {0, STEP}, {0, -STEP},
                    {STEP, STEP}, {STEP, -STEP}, {-STEP, STEP}, {-STEP, -STEP}}) {
                int nx = cx + dir[0];
                int nz = cz + dir[1];
                if (nx < minX || nx > maxX || nz < minZ || nz > maxZ) {
                    continue;
                }
                if (!terrain.passable(nx, nz)) {
                    continue;
                }
                long nk = key(nx, nz);
                double tentative = currentG + terrain.moveCost(cx, cz, nx, nz);
                if (tentative < gScore.getOrDefault(nk, Double.MAX_VALUE)) {
                    gScore.put(nk, tentative);
                    cameFrom.put(nk, current);
                    double f = tentative + heuristic(nx, nz, alignedGoalX, alignedGoalZ);
                    open.add(new long[]{nk, Double.doubleToLongBits(f)});
                }
            }
        }
        return List.of();
    }

    private static List<RoadNetwork.Node> reconstruct(Map<Long, Long> cameFrom, long current) {
        List<RoadNetwork.Node> path = new ArrayList<>();
        Long cursor = current;
        while (cursor != null) {
            path.add(new RoadNetwork.Node((int) (cursor.longValue() >> 32), cursor.intValue()));
            cursor = cameFrom.get(cursor);
        }
        java.util.Collections.reverse(path);
        return path;
    }

    private static double heuristic(int x, int z, int goalX, int goalZ) {
        return (Math.abs(goalX - x) + Math.abs(goalZ - z)) / (double) STEP;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }
}
