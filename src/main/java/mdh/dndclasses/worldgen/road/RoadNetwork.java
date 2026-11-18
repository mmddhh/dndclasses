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
        int width = 160;
        int height = 64;
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

    /** Self-contained SVG so the road topology can be inspected without a manual legend script. */
    public String toSvg(WorldPlan plan, TerrainCost terrain) {
        int width = 1200;
        int height = 700;
        int margin = 30;
        List<WorldPlan.Settlement> settlements = plan.settlements();
        if (settlements.isEmpty()) {
            return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"400\" height=\"80\">"
                    + "<text x=\"10\" y=\"30\">no settlements</text></svg>";
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
        double scale = Math.min((width - 2.0 * margin) / (maxX - minX),
                (height - 2.0 * margin) / (maxZ - minZ));
        double offsetX = margin;
        double offsetY = margin;
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
                .append("\" height=\"").append(height).append("\">");
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#4a90d9\"/>");

        int cellsX = 160;
        int cellsZ = 90;
        double cellW = (maxX - minX) / (double) cellsX;
        double cellH = (maxZ - minZ) / (double) cellsZ;
        for (int gz = 0; gz < cellsZ; gz++) {
            for (int gx = 0; gx < cellsX; gx++) {
                int x = minX + (int) (gx * cellW);
                int z = minZ + (int) (gz * cellH);
                if (terrain.passable(x, z)) {
                    svg.append("<rect x=\"").append(offsetX + gx * cellW * scale)
                            .append("\" y=\"").append(offsetY + gz * cellH * scale)
                            .append("\" width=\"").append(cellW * scale + 0.5)
                            .append("\" height=\"").append(cellH * scale + 0.5)
                            .append("\" fill=\"#7bbf6a\"/>");
                }
            }
        }

        for (Road road : roads) {
            String color = switch (road.tier()) {
                case HIGHWAY -> "#f2c14e";
                case ROAD -> "#b8b8b8";
                case PATH -> "#c47a3a";
            };
            svg.append("<polyline fill=\"none\" stroke=\"").append(color)
                    .append("\" stroke-width=\"").append(road.tier() == Tier.HIGHWAY ? 4
                            : road.tier() == Tier.ROAD ? 3 : 2)
                    .append("\" stroke-linecap=\"round\" stroke-linejoin=\"round\" points=\"");
            for (Node node : road.path()) {
                svg.append(offsetX + (node.x() - minX) * scale).append(',')
                        .append(offsetY + (node.z() - minZ) * scale).append(' ');
            }
            svg.append("\"/>");
        }
        for (FailedRoute failed : failedRoutes) {
            svg.append("<line x1=\"").append(offsetX + (failed.fromX() - minX) * scale)
                    .append("\" y1=\"").append(offsetY + (failed.fromZ() - minZ) * scale)
                    .append("\" x2=\"").append(offsetX + (failed.toX() - minX) * scale)
                    .append("\" y2=\"").append(offsetY + (failed.toZ() - minZ) * scale)
                    .append("\" stroke=\"#ff3b30\" stroke-width=\"2\" stroke-dasharray=\"7 5\"/>");
        }
        for (WorldPlan.Settlement settlement : settlements) {
            boolean capital = settlement.id() == plan.settlements().stream()
                    .filter(candidate -> candidate.nationId() == settlement.nationId())
                    .mapToInt(WorldPlan.Settlement::id).min().orElse(-1);
            svg.append("<circle cx=\"").append(offsetX + (settlement.x() - minX) * scale)
                    .append("\" cy=\"").append(offsetY + (settlement.z() - minZ) * scale)
                    .append("\" r=\"").append(capital ? 6 : 4)
                    .append("\" fill=\"").append(capital ? "#d64545" : "#111111")
                    .append("\" stroke=\"#ffffff\" stroke-width=\"1\"/>");
        }
        for (BorderPost post : borderPosts) {
            svg.append("<rect x=\"").append(offsetX + (post.x() - minX) * scale - 4)
                    .append("\" y=\"").append(offsetY + (post.z() - minZ) * scale - 4)
                    .append("\" width=\"8\" height=\"8\" fill=\"#9b59b6\"/>");
        }
        svg.append("<rect x=\"10\" y=\"10\" width=\"330\" height=\"76\" fill=\"#ffffff\"")
                .append(" fill-opacity=\"0.85\" stroke=\"#333\"/>");
        svg.append("<text x=\"20\" y=\"30\" font-family=\"monospace\" font-size=\"13\">")
                .append("gold=highway  silver=road  copper=path</text>");
        svg.append("<text x=\"20\" y=\"50\" font-family=\"monospace\" font-size=\"13\">")
                .append("red=capital  black=town  purple=border post</text>");
        svg.append("<text x=\"20\" y=\"70\" font-family=\"monospace\" font-size=\"13\">")
                .append("green=land  blue=water  red dashed=no path/failed</text>");
        svg.append("</svg>");
        return svg.toString();
    }
}
