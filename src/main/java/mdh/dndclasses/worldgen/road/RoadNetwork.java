package mdh.dndclasses.worldgen.road;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/** Pure road/route data; no Minecraft imports. */
public record RoadNetwork(List<Road> roads, List<BorderPost> borderPosts) {

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

    public String prettyJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(toJson());
    }

    public JsonObject toJson() {        JsonObject root = new JsonObject();
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
        return root;
    }
}
