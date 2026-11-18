package mdh.dndclasses.worldgen.road;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mdh.dndclasses.worldgen.WorldPlan;

import java.util.ArrayList;
import java.util.List;

/** Pure road/route data; no Minecraft imports. */
public record RoadNetwork(List<Road> roads, List<BorderPost> borderPosts,
                          List<FailedRoute> failedRoutes) {

    public enum Tier {
        HIGHWAY(5, "highway"),
        ROAD(3, "road"),
        PATH(2, "path");

        private final int width;
        private final String id;

        Tier(int width, String id) {
            this.width = width;
            this.id = id;
        }

        public int width() {
            return width;
        }

        public String id() {
            return id;
        }
    }

    public record Node(int x, int z) {
    }

    public record Road(int id, Tier tier, int nationA, int nationB, List<Node> path,
                       String surface) {
    }

    public record BorderPost(int x, int z, int nationA, int nationB) {
    }

    public record FailedRoute(int nationA, int nationB, int fromX, int fromZ, int toX, int toZ,
                              String reason) {
    }

    public String prettyJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(toJson());
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        JsonArray roadArray = new JsonArray();
        for (Road road : roads) {
            JsonObject json = new JsonObject();
            json.addProperty("id", road.id());
            json.addProperty("tier", road.tier().id());
            json.addProperty("width", road.tier().width());
            json.addProperty("nationA", road.nationA());
            json.addProperty("nationB", road.nationB());
            json.addProperty("surface", road.surface());
            JsonArray path = new JsonArray();
            for (Node node : road.path()) {
                JsonObject point = new JsonObject();
                point.addProperty("x", node.x());
                point.addProperty("z", node.z());
                path.add(point);
            }
            json.add("path", path);
            roadArray.add(json);
        }
        root.add("roads", roadArray);

        JsonArray postArray = new JsonArray();
        for (BorderPost post : borderPosts) {
            JsonObject json = new JsonObject();
            json.addProperty("x", post.x());
            json.addProperty("z", post.z());
            json.addProperty("nationA", post.nationA());
            json.addProperty("nationB", post.nationB());
            postArray.add(json);
        }
        root.add("borderPosts", postArray);

        JsonArray failedArray = new JsonArray();
        for (FailedRoute failed : failedRoutes) {
            JsonObject json = new JsonObject();
            json.addProperty("nationA", failed.nationA());
            json.addProperty("nationB", failed.nationB());
            json.addProperty("fromX", failed.fromX());
            json.addProperty("fromZ", failed.fromZ());
            json.addProperty("toX", failed.toX());
            json.addProperty("toZ", failed.toZ());
            json.addProperty("reason", failed.reason());
            failedArray.add(json);
        }
        root.add("failedRoutes", failedArray);
        return root;
    }

    public List<String> debugMap(WorldPlan plan, TerrainCost terrain) {
        int width = 96;
        int height = 44;
        List<WorldPlan.Settlement> settlements = plan.settlements();
        if (settlements.isEmpty()) {
            return List.of("(no settlements)");
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (WorldPlan.Settlement settlement : settlements) {
            minX = Math.min(minX, settlement.x());
            maxX = Math.max(maxX, settlement.x());
            minZ = Math.min(minZ, settlement.z());
            maxZ = Math.max(maxZ, settlement.z());
        }
        minX -= 512;
        maxX += 512;
        minZ -= 512;
        maxZ += 512;
        double scaleX = Math.max(1.0D, (maxX - minX) / (double) width);
        double scaleZ = Math.max(1.0D, (maxZ - minZ) / (double) height);

        char[][] grid = new char[height][width];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int x = minX + (int) (col * scaleX);
                int z = minZ + (int) (row * scaleZ);
                grid[row][col] = terrain.passable(x, z) ? '.' : '~';
            }
        }
        for (Road road : roads) {
            char mark = switch (road.tier()) {
                case HIGHWAY -> '#';
                case ROAD -> '+';
                case PATH -> '-';
            };
            for (int i = 1; i < road.path().size(); i++) {
                Node a = road.path().get(i - 1);
                Node b = road.path().get(i);
                int steps = Math.max(1, (int) (Math.max(Math.abs(b.x() - a.x()),
                        Math.abs(b.z() - a.z())) / Math.min(scaleX, scaleZ) / 2));
                for (int s = 0; s <= steps; s++) {
                    int x = a.x() + (b.x() - a.x()) * s / steps;
                    int z = a.z() + (b.z() - a.z()) * s / steps;
                    plot(grid, x, z, minX, minZ, scaleX, scaleZ, mark);
                }
            }
        }
        for (WorldPlan.Settlement settlement : settlements) {
            boolean capital = settlement.id() == plan.settlements().stream()
                    .filter(candidate -> candidate.nationId() == settlement.nationId())
                    .mapToInt(WorldPlan.Settlement::id).min().orElse(-1);
            plot(grid, settlement.x(), settlement.z(), minX, minZ, scaleX, scaleZ,
                    capital ? 'C' : 't');
        }
        for (BorderPost post : borderPosts) {
            plot(grid, post.x(), post.z(), minX, minZ, scaleX, scaleZ, 'B');
        }

        List<String> lines = new ArrayList<>();
        lines.add("legend: # highway  + road  - path  C capital  t town  B border post  ~ water");
        lines.add("bounds: x " + minX + ".." + maxX + " z " + minZ + ".." + maxZ);
        for (char[] row : grid) {
            lines.add(new String(row));
        }
        return lines;
    }

    private static void plot(char[][] grid, int x, int z, int minX, int minZ,
                             double scaleX, double scaleZ, char mark) {
        int col = (int) ((x - minX) / scaleX);
        int row = (int) ((z - minZ) / scaleZ);
        if (row >= 0 && row < grid.length && col >= 0 && col < grid[0].length) {
            grid[row][col] = mark;
        }
    }
}
