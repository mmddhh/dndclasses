package mdh.dndclasses.worldgen.mc;

import mdh.dndclasses.worldgen.WorldPlan;
import mdh.dndclasses.worldgen.WorldPlanGenerator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * World-scoped persistence for the plan. The plan is generated once per world seed and then loaded
 * from the overworld's data storage; commands and future placement adapters read the same instance.
 */
public final class WorldPlanSavedData extends SavedData {

    private static final String NAME = "dndclasses_worldplan";
    private static final String TAG_SEED = "seed";
    private static final String TAG_NATIONS = "nations";
    private static final String TAG_SETTLEMENTS = "settlements";
    private static final String TAG_EVENTS = "events";

    private WorldPlan plan;

    private WorldPlanSavedData(WorldPlan plan) {
        this.plan = plan;
    }

    public static WorldPlanSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                WorldPlanSavedData::load,
                () -> new WorldPlanSavedData(WorldPlanGenerator.generate(level.getSeed())),
                NAME);
    }

    public WorldPlan plan() {
        return plan;
    }

    public void replace(WorldPlan newPlan) {
        this.plan = newPlan;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong(TAG_SEED, plan.seed());

        ListTag nations = new ListTag();
        for (WorldPlan.Nation nation : plan.nations()) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("id", nation.id());
            entry.putString("name", nation.name());
            entry.putString("race", nation.race());
            entry.putString("government", nation.government());
            entry.putInt("military", nation.military());
            entry.putInt("wealth", nation.wealth());
            entry.putString("culture", nation.culture());
            nations.add(entry);
        }
        tag.put(TAG_NATIONS, nations);

        ListTag settlements = new ListTag();
        for (WorldPlan.Settlement settlement : plan.settlements()) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("id", settlement.id());
            entry.putString("name", settlement.name());
            entry.putInt("nation", settlement.nationId());
            entry.putInt("x", settlement.x());
            entry.putInt("z", settlement.z());
            entry.putInt("population", settlement.population());
            entry.putString("culture", settlement.culture());
            ListTag materials = new ListTag();
            for (String material : settlement.materials()) {
                materials.add(net.minecraft.nbt.StringTag.valueOf(material));
            }
            entry.put("materials", materials);
            settlements.add(entry);
        }
        tag.put(TAG_SETTLEMENTS, settlements);

        ListTag events = new ListTag();
        for (WorldPlan.HistoryEvent event : plan.events()) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("year", event.year());
            entry.putString("type", event.type());
            entry.putInt("nationA", event.nationA());
            entry.putInt("nationB", event.nationB());
            entry.putInt("settlement", event.settlementId());
            entry.putString("summary", event.summary());
            events.add(entry);
        }
        tag.put(TAG_EVENTS, events);
        return tag;
    }

    private static WorldPlanSavedData load(CompoundTag tag) {
        long seed = tag.getLong(TAG_SEED);

        List<WorldPlan.Nation> nations = new ArrayList<>();
        ListTag nationList = tag.getList(TAG_NATIONS, Tag.TAG_COMPOUND);
        for (int i = 0; i < nationList.size(); i++) {
            CompoundTag entry = nationList.getCompound(i);
            nations.add(new WorldPlan.Nation(entry.getInt("id"), entry.getString("name"),
                    entry.getString("race"), entry.getString("government"),
                    entry.getInt("military"), entry.getInt("wealth"), entry.getString("culture")));
        }

        List<WorldPlan.Settlement> settlements = new ArrayList<>();
        ListTag settlementList = tag.getList(TAG_SETTLEMENTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < settlementList.size(); i++) {
            CompoundTag entry = settlementList.getCompound(i);
            List<String> materials = new ArrayList<>();
            ListTag materialList = entry.getList("materials", Tag.TAG_STRING);
            for (int m = 0; m < materialList.size(); m++) {
                materials.add(materialList.getString(m));
            }
            settlements.add(new WorldPlan.Settlement(entry.getInt("id"), entry.getString("name"),
                    entry.getInt("nation"), entry.getInt("x"), entry.getInt("z"),
                    entry.getInt("population"), entry.getString("culture"), List.copyOf(materials)));
        }

        List<WorldPlan.HistoryEvent> events = new ArrayList<>();
        ListTag eventList = tag.getList(TAG_EVENTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < eventList.size(); i++) {
            CompoundTag entry = eventList.getCompound(i);
            events.add(new WorldPlan.HistoryEvent(entry.getInt("year"), entry.getString("type"),
                    entry.getInt("nationA"), entry.getInt("nationB"), entry.getInt("settlement"),
                    entry.getString("summary")));
        }

        return new WorldPlanSavedData(new WorldPlan(seed, List.copyOf(nations),
                List.copyOf(settlements), List.copyOf(events)));
    }
}
