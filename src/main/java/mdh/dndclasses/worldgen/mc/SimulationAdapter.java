package mdh.dndclasses.worldgen.mc;

import mdh.dndclasses.worldgen.WorldPlan;
import mdh.dndclasses.worldgen.sim.SimulationResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts the Minecraft-free civilization simulation into the persisted WorldPlan shape. This is
 * the bridge that makes the simulated nations/settlements actually drive world placement.
 */
public final class SimulationAdapter {

    private SimulationAdapter() {
    }

    public static WorldPlan adapt(SimulationResult simulation) {
        Map<Integer, SimulationResult.Nation> simNations = new HashMap<>();
        for (SimulationResult.Nation nation : simulation.nations()) {
            simNations.put(nation.id(), nation);
        }

        List<WorldPlan.Nation> nations = new ArrayList<>();
        for (SimulationResult.Nation nation : simulation.nations()) {
            int military = Math.max(10, nation.cells() / 3);
            int wealth = Math.max(20, nation.cells() / 2);
            nations.add(new WorldPlan.Nation(nation.id(), nation.name(), nation.race(),
                    governmentFor(nation.race()), military, wealth, nation.culture()));
        }

        Map<Integer, SimulationResult.Settlement> settlementsById = new HashMap<>();
        Set<String> usedNames = new HashSet<>();
        List<WorldPlan.Settlement> settlements = new ArrayList<>();
        for (SimulationResult.Settlement settlement : simulation.settlements()) {
            SimulationResult.Nation nation = simNations.get(settlement.nationId());
            if (nation == null) {
                continue;
            }
            String name = uniqueName(settlement.name(), usedNames);
            WorldPlan.Settlement converted = new WorldPlan.Settlement(settlements.size(), name,
                    settlement.nationId(), settlement.x(), settlement.z(), settlement.population(),
                    settlement.culture(), materialsFor(nation.race()));
            settlements.add(converted);
            settlementsById.put(converted.id(), settlement);
        }

        Map<Integer, Integer> firstSettlement = new HashMap<>();
        for (WorldPlan.Settlement settlement : settlements) {
            firstSettlement.putIfAbsent(settlement.nationId(), settlement.id());
        }

        List<WorldPlan.HistoryEvent> events = new ArrayList<>();
        Map<String, Integer> lastEventYear = new HashMap<>();
        for (SimulationResult.Event event : simulation.events()) {
            if (!simNations.containsKey(event.nationA())) {
                continue;
            }
            String type = switch (event.type()) {
                case "expand", "war", "plague" -> event.type();
                default -> null;
            };
            if (type == null) {
                continue;
            }
            int nationB = "expand".equals(type) || "plague".equals(type) ? -1 : event.nationB();
            if (nationB != -1 && !simNations.containsKey(nationB)) {
                continue;
            }
            int settlementId = firstSettlement.getOrDefault(event.nationA(), -1);
            if (settlementId < 0) {
                continue;
            }
            int minGap = switch (type) {
                case "war" -> 15;
                case "expand" -> 0;
                default -> 0;
            };
            String key = Math.min(event.nationA(), nationB) + ":" + Math.max(event.nationA(), nationB)
                    + ":" + type;
            Integer previous = lastEventYear.get(key);
            if (minGap > 0 && previous != null && event.year() - previous < minGap) {
                continue;
            }
            lastEventYear.put(key, event.year());
            events.add(new WorldPlan.HistoryEvent(event.year(), type, event.nationA(), nationB,
                    settlementId, event.summary()));
        }

        return new WorldPlan(simulation.seed(), List.copyOf(nations), List.copyOf(settlements),
                List.copyOf(events));
    }

    private static String uniqueName(String base, Set<String> used) {
        if (used.add(base)) {
            return base;
        }
        int suffix = 2;
        while (!used.add(base + " " + suffix)) {
            suffix++;
        }
        return base + " " + suffix;
    }

    private static String governmentFor(String race) {
        return switch (race) {
            case "elf" -> "council";
            case "dwarf" -> "clan";
            case "orc" -> "warband";
            case "halfling" -> "elders";
            case "dragonborn" -> "dragonflight";
            default -> "monarchy";
        };
    }

    private static List<String> materialsFor(String race) {
        return switch (race) {
            case "elf" -> List.of("oak", "spruce");
            case "dwarf" -> List.of("deepslate", "stone");
            case "orc" -> List.of("deepslate", "copper");
            case "halfling" -> List.of("oak", "earth");
            case "dragonborn" -> List.of("sandstone", "copper");
            default -> List.of("stone", "oak");
        };
    }
}
