package mdh.dndclasses.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Deterministic, Minecraft-free world plan generator. Same seed = same nations, settlements and
 * history. M0.5 adds nation-level culture, state-changing history events and event constraints.
 */
public final class WorldPlanGenerator {

    private static final String[] RACES = {"human", "elf", "dwarf", "orc", "halfling", "dragonborn"};
    private static final String[] GOVERNMENTS = {"kingdom", "republic", "theocracy", "clan", "magocracy"};
    private static final String[] ROOFS = {"steep", "flat", "domed", "gabled", "spiral"};
    private static final String[] WINDOWS = {"small", "tall", "arched", "round"};
    private static final String[] MOTIFS = {"rune", "vine", "star", "dragon", "hammer", "wave"};
    private static final String[] TRIMS = {"carved", "painted", "plain", "gilded"};
    private static final String[] MATERIALS = {"oak", "spruce", "stone", "deepslate", "sandstone", "copper"};

    private static final String[] NAME_START = {"Aer", "Bel", "Cor", "Dun", "Eld", "Fen", "Gal",
            "Hal", "Ith", "Kar", "Lor", "Mor", "Nal", "Or", "Pel", "Quel", "Rav", "Syl", "Tor", "Ul"};
    private static final String[] NAME_END = {"dor", "heim", "vale", "mere", "gard", "fell", "wood",
            "hold", "reach", "spire", "moor", "ford", "keep", "hall"};

    private WorldPlanGenerator() {
    }

    public static WorldPlan generate(long seed) {
        Random rng = new Random(seed);
        int nationCount = 6;
        int settlementsPerNation = 5;

        List<NationState> nations = new ArrayList<>();
        for (int i = 0; i < nationCount; i++) {
            String race = RACES[i % RACES.length];
            String culture = "roof=" + pick(rng, ROOFS)
                    + ";window=" + pick(rng, WINDOWS)
                    + ";motif=" + pick(rng, MOTIFS);
            nations.add(new NationState(i, nationName(rng, race), race,
                    GOVERNMENTS[rng.nextInt(GOVERNMENTS.length)],
                    30 + rng.nextInt(70), 30 + rng.nextInt(70), culture));
        }

        List<SettlementState> settlements = new ArrayList<>();
        Set<String> usedSettlementNames = new HashSet<>();
        for (int nationId = 0; nationId < nationCount; nationId++) {
            NationState nation = nations.get(nationId);
            for (int s = 0; s < settlementsPerNation; s++) {
                int id = settlements.size();
                int x = (nationId - nationCount / 2) * 512 + (s % 2) * 180 + rng.nextInt(80);
                int z = (s / 2) * 420 - 210 + rng.nextInt(80);
                String name = uniqueSettlementName(rng, usedSettlementNames);
                String culture = nation.culture + ";trim=" + pick(rng, TRIMS);
                List<String> materials = preferencesFor(nation.race);
                SettlementState settlement = new SettlementState(id, name, nationId, x, z,
                        40 + rng.nextInt(260), culture, materials);
                settlements.add(settlement);
                nation.settlements.add(settlement);
            }
        }

        List<WorldPlan.HistoryEvent> events = simulateHistory(rng, nations, settlements);
        List<WorldPlan.Nation> nationRecords = nations.stream().map(NationState::toRecord).toList();
        List<WorldPlan.Settlement> settlementRecords = settlements.stream().map(SettlementState::toRecord).toList();
        return new WorldPlan(seed, nationRecords, settlementRecords, List.copyOf(events));
    }

    private static List<WorldPlan.HistoryEvent> simulateHistory(Random rng, List<NationState> nations,
                                                                List<SettlementState> settlements) {
        List<WorldPlan.HistoryEvent> events = new ArrayList<>();
        Map<Long, Integer> lastWar = new HashMap<>();
        Map<Long, Integer> lastAlliance = new HashMap<>();
        Map<Long, Integer> lastTrade = new HashMap<>();

        for (int year = 5; year <= 150; year += 5) {
            int a = rng.nextInt(nations.size());
            int b = rng.nextInt(nations.size());
            if (a == b) {
                b = (b + 1) % nations.size();
            }
            NationState nationA = nations.get(a);
            NationState nationB = nations.get(b);
            long pair = pairKey(a, b);
            boolean warReady = year - lastWar.getOrDefault(pair, -999) >= 15;
            boolean allianceReady = year - lastAlliance.getOrDefault(pair, -999) >= 40;
            boolean tradeReady = year - lastTrade.getOrDefault(pair, -999) >= 20;
            int roll = rng.nextInt(100);

            if (warReady && roll < 30) {
                NationState winner = nationA.military + rng.nextInt(20) >= nationB.military + rng.nextInt(20)
                        ? nationA : nationB;
                NationState loser = winner == nationA ? nationB : nationA;
                SettlementState site = loser.randomSettlement(rng);
                int loss = 10 + rng.nextInt(16);
                loser.military = Math.max(10, loser.military - loss);
                loser.wealth = Math.max(5, loser.wealth - 5 - rng.nextInt(16));
                winner.wealth = Math.min(100, winner.wealth + 5 + rng.nextInt(11));
                winner.military = Math.min(100, winner.military + 3 + rng.nextInt(8));
                if (site != null) {
                    site.population = Math.max(8, site.population - site.population * (10 + rng.nextInt(21)) / 100);
                }
                lastWar.put(pair, year);
                String summary = nationA.name + " 与 " + nationB.name + " 爆发战争，" + winner.name
                        + " 获胜" + (site == null ? "。" : "，" + site.name + " 遭到洗劫。");
                events.add(new WorldPlan.HistoryEvent(year, "war", a, b,
                        site == null ? -1 : site.id, summary));
            } else if (allianceReady && roll < 55) {
                nationA.wealth = Math.min(100, nationA.wealth + 2);
                nationB.wealth = Math.min(100, nationB.wealth + 2);
                lastAlliance.put(pair, year);
                SettlementState site = rng.nextBoolean() ? nationA.randomSettlement(rng) : nationB.randomSettlement(rng);
                events.add(new WorldPlan.HistoryEvent(year, "alliance", a, b,
                        site == null ? -1 : site.id,
                        nationA.name + " 与 " + nationB.name + " 缔结盟约。"));
            } else if (tradeReady) {
                nationA.wealth = Math.min(100, nationA.wealth + 3);
                nationB.wealth = Math.min(100, nationB.wealth + 3);
                lastTrade.put(pair, year);
                SettlementState site = nationA.randomSettlement(rng);
                if (site != null) {
                    String material = pick(rng, MATERIALS);
                    if (!site.materials.contains(material) && site.materials.size() < 3) {
                        site.materials.add(material);
                    }
                }
                events.add(new WorldPlan.HistoryEvent(year, "trade", a, b,
                        site == null ? -1 : site.id,
                        nationA.name + " 与 " + nationB.name + " 开通商路。"));
            } else {
                nationA.wealth = Math.max(5, nationA.wealth - 6 - rng.nextInt(10));
                SettlementState site = nationA.randomSettlement(rng);
                if (site != null) {
                    site.population = Math.max(8, site.population - site.population * (20 + rng.nextInt(21)) / 100);
                }
                events.add(new WorldPlan.HistoryEvent(year, "plague", a, -1,
                        site == null ? -1 : site.id,
                        nationA.name + " 爆发瘟疫，人口锐减。"));
            }

            // Keep the settlement list stable but make sure event sites point at existing ids.
            int eventIndex = events.size() - 1;
            if (eventIndex >= 0) {
                WorldPlan.HistoryEvent event = events.get(eventIndex);
                if (event.settlementId() >= settlements.size()) {
                    events.set(eventIndex, new WorldPlan.HistoryEvent(event.year(), event.type(),
                            event.nationA(), event.nationB(), -1, event.summary()));
                }
            }
        }
        return events;
    }

    /** A single materialized NPC record for one settlement. */
    public static NpcProfile npcFor(WorldPlan plan, WorldPlan.Settlement settlement, int index) {
        Random rng = new Random(plan.seed() ^ (settlement.id() * 31L) ^ (index * 131L));
        WorldPlan.Nation nation = plan.nations().get(settlement.nationId());
        String role = switch (index % 4) {
            case 0 -> "merchant";
            case 1 -> "guard";
            case 2 -> "scholar";
            default -> "priest";
        };
        return new NpcProfile(personName(rng, nation.race()), nation.race(), role,
                settlement.id(), nation.id(), settlement.culture(), role);
    }

    private static long pairKey(int a, int b) {
        int low = Math.min(a, b);
        int high = Math.max(a, b);
        return ((long) low << 32) | (high & 0xffffffffL);
    }

    private static List<String> preferencesFor(String race) {
        return switch (race) {
            case "elf" -> List.of("oak", "spruce");
            case "dwarf" -> List.of("deepslate", "stone");
            case "orc" -> List.of("deepslate", "copper");
            case "halfling" -> List.of("oak", "earth");
            case "dragonborn" -> List.of("sandstone", "copper");
            default -> List.of("stone", "oak");
        };
    }

    private static String uniqueSettlementName(Random rng, Set<String> used) {
        for (int attempt = 0; attempt < 20; attempt++) {
            String name = settlementName(rng);
            if (used.add(name)) {
                return name;
            }
        }
        return settlementName(rng) + Integer.toString(used.size());
    }

    private static String nationName(Random rng, String race) {
        String prefix = switch (race) {
            case "elf" -> "Sylvan";
            case "dwarf" -> "Iron";
            case "orc" -> "Grim";
            case "halfling" -> "Green";
            case "dragonborn" -> "Draco";
            default -> "Crown";
        };
        return prefix + " " + capitalize(pick(rng, NAME_END));
    }

    private static String settlementName(Random rng) {
        return capitalize(pick(rng, NAME_START)) + pick(rng, NAME_END);
    }

    private static String personName(Random rng, String race) {
        String first = capitalize(pick(rng, NAME_START)) + (race.equals("elf") ? "iel" : "");
        return first + " " + capitalize(pick(rng, NAME_END));
    }

    private static String pick(Random rng, String[] values) {
        return values[rng.nextInt(values.length)];
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static final class NationState {
        private final int id;
        private final String name;
        private final String race;
        private final String government;
        private int military;
        private int wealth;
        private final String culture;
        private final List<SettlementState> settlements = new ArrayList<>();

        private NationState(int id, String name, String race, String government, int military,
                            int wealth, String culture) {
            this.id = id;
            this.name = name;
            this.race = race;
            this.government = government;
            this.military = military;
            this.wealth = wealth;
            this.culture = culture;
        }

        private SettlementState randomSettlement(Random rng) {
            return settlements.isEmpty() ? null : settlements.get(rng.nextInt(settlements.size()));
        }

        private WorldPlan.Nation toRecord() {
            return new WorldPlan.Nation(id, name, race, government, military, wealth, culture);
        }
    }

    private static final class SettlementState {
        private final int id;
        private final String name;
        private final int nationId;
        private final int x;
        private final int z;
        private int population;
        private final String culture;
        private final List<String> materials;

        private SettlementState(int id, String name, int nationId, int x, int z, int population,
                                String culture, List<String> materials) {
            this.id = id;
            this.name = name;
            this.nationId = nationId;
            this.x = x;
            this.z = z;
            this.population = population;
            this.culture = culture;
            this.materials = new ArrayList<>(materials);
        }

        private WorldPlan.Settlement toRecord() {
            return new WorldPlan.Settlement(id, name, nationId, x, z, population, culture,
                    List.copyOf(materials));
        }
    }
}
