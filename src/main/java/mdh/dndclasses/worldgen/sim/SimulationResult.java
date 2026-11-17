package mdh.dndclasses.worldgen.sim;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/** Pure-data result of the civilization simulation prototype. */
public record SimulationResult(long seed, int gridSize, int cellSize,
                              List<Nation> nations, List<Settlement> settlements,
                              List<Event> events, List<String> map) {

    public record Nation(int id, String name, String race, String culture,
                         int cells, int capitalX, int capitalZ) {
    }

    public record Settlement(int id, String name, int nationId, int x, int z, int population,
                             String culture) {
    }

    public record Event(int year, String type, int nationA, int nationB, String summary) {
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("seed", seed);
        root.addProperty("gridSize", gridSize);
        root.addProperty("cellSize", cellSize);

        JsonArray nationArray = new JsonArray();
        for (Nation nation : nations) {
            JsonObject json = new JsonObject();
            json.addProperty("id", nation.id());
            json.addProperty("name", nation.name());
            json.addProperty("race", nation.race());
            json.addProperty("culture", nation.culture());
            json.addProperty("cells", nation.cells());
            json.addProperty("capitalX", nation.capitalX());
            json.addProperty("capitalZ", nation.capitalZ());
            nationArray.add(json);
        }
        root.add("nations", nationArray);

        JsonArray settlementArray = new JsonArray();
        for (Settlement settlement : settlements) {
            JsonObject json = new JsonObject();
            json.addProperty("id", settlement.id());
            json.addProperty("name", settlement.name());
            json.addProperty("nation", settlement.nationId());
            json.addProperty("x", settlement.x());
            json.addProperty("z", settlement.z());
            json.addProperty("population", settlement.population());
            json.addProperty("culture", settlement.culture());
            settlementArray.add(json);
        }
        root.add("settlements", settlementArray);

        JsonArray eventArray = new JsonArray();
        for (Event event : events) {
            JsonObject json = new JsonObject();
            json.addProperty("year", event.year());
            json.addProperty("type", event.type());
            json.addProperty("nationA", event.nationA());
            json.addProperty("nationB", event.nationB());
            json.addProperty("summary", event.summary());
            eventArray.add(json);
        }
        root.add("events", eventArray);

        JsonArray mapArray = new JsonArray();
        for (String row : map) {
            mapArray.add(row);
        }
        root.add("map", mapArray);
        return root;
    }
}
