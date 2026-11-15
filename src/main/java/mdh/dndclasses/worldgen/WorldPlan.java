package mdh.dndclasses.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Pure-data world plan. Deliberately has no Minecraft imports: it can be generated off-thread and
 * serialized without touching a Level. Placement adapters consume it read-only.
 */
public record WorldPlan(long seed, List<Nation> nations, List<Settlement> settlements, List<HistoryEvent> events) {

    public record Nation(int id, String name, String race, String government, int military, int wealth) {
    }

    public record Settlement(int id, String name, int nationId, int x, int z, int population,
                             String culture, List<String> materials) {
    }

    public record HistoryEvent(int year, String type, int nationA, int nationB, int settlementId,
                               String summary) {
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("seed", seed);

        JsonArray nationArray = new JsonArray();
        for (Nation nation : nations) {
            JsonObject json = new JsonObject();
            json.addProperty("id", nation.id());
            json.addProperty("name", nation.name());
            json.addProperty("race", nation.race());
            json.addProperty("government", nation.government());
            json.addProperty("military", nation.military());
            json.addProperty("wealth", nation.wealth());
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
            JsonArray materials = new JsonArray();
            settlement.materials().forEach(materials::add);
            json.add("materials", materials);
            settlementArray.add(json);
        }
        root.add("settlements", settlementArray);

        JsonArray eventArray = new JsonArray();
        for (HistoryEvent event : events) {
            JsonObject json = new JsonObject();
            json.addProperty("year", event.year());
            json.addProperty("type", event.type());
            json.addProperty("nationA", event.nationA());
            json.addProperty("nationB", event.nationB());
            json.addProperty("settlement", event.settlementId());
            json.addProperty("summary", event.summary());
            eventArray.add(json);
        }
        root.add("events", eventArray);
        return root;
    }
}
