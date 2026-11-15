package mdh.dndclasses.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic, Minecraft-free world plan generator. Same seed = same nations, settlements and
 * history. This is intentionally a small prototype of the planning layer, not the final scale.
 */
public final class WorldPlanGenerator {

    private static final String[] RACES = {"human", "elf", "dwarf", "orc", "halfling", "dragonborn"};
    private static final String[] GOVERNMENTS = {"kingdom", "republic", "theocracy", "clan", "magocracy"};
    private static final String[] ROOFS = {"steep", "flat", "domed", "gabled", "spiral"};
    private static final String[] WINDOWS = {"small", "tall", "arched", "round"};
    private static final String[] MOTIFS = {"rune", "vine", "star", "dragon", "hammer", "wave"};
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

        List<WorldPlan.Nation> nations = new ArrayList<>();
        for (int i = 0; i < nationCount; i++) {
            String race = RACES[i % RACES.length];
            nations.add(new WorldPlan.Nation(i, nationName(rng, race), race,
                    GOVERNMENTS[rng.nextInt(GOVERNMENTS.length)],
                    30 + rng.nextInt(70), 30 + rng.nextInt(70)));
        }

        List<WorldPlan.Settlement> settlements = new ArrayList<>();
        for (int nationId = 0; nationId < nationCount; nationId++) {
            for (int s = 0; s < settlementsPerNation; s++) {
                int id = settlements.size();
                int x = (nationId - nationCount / 2) * 512 + (s % 2) * 180 + rng.nextInt(80);
                int z = (s / 2) * 420 - 210 + rng.nextInt(80);
                String culture = "roof=" + pick(rng, ROOFS)
                        + ";window=" + pick(rng, WINDOWS)
                        + ";motif=" + pick(rng, MOTIFS);
                List<String> materials = new ArrayList<>();
                materials.add(pick(rng, MATERIALS));
                materials.add(pick(rng, MATERIALS));
                settlements.add(new WorldPlan.Settlement(id, settlementName(rng), nationId, x, z,
                        40 + rng.nextInt(260), culture, List.copyOf(materials)));
            }
        }

        List<WorldPlan.HistoryEvent> events = new ArrayList<>();
        for (int year = 5; year <= 150; year += 5) {
            int a = rng.nextInt(nationCount);
            int b = rng.nextInt(nationCount);
            if (a == b) {
                b = (b + 1) % nationCount;
            }
            WorldPlan.Nation nationA = nations.get(a);
            WorldPlan.Nation nationB = nations.get(b);
            String type;
            String summary;
            int roll = rng.nextInt(100);
            if (roll < 25) {
                type = "war";
                String winner = nationA.military() >= nationB.military() ? nationA.name() : nationB.name();
                summary = nationA.name() + " 与 " + nationB.name() + " 爆发战争，" + winner + " 获胜。";
            } else if (roll < 50) {
                type = "alliance";
                summary = nationA.name() + " 与 " + nationB.name() + " 缔结盟约。";
            } else if (roll < 75) {
                type = "trade";
                summary = nationA.name() + " 与 " + nationB.name() + " 开通商路。";
            } else {
                type = "plague";
                summary = nationA.name() + " 爆发瘟疫，人口锐减。";
            }
            int settlementId = settlements.get(rng.nextInt(settlements.size())).id();
            events.add(new WorldPlan.HistoryEvent(year, type, a, b, settlementId, summary));
        }

        return new WorldPlan(seed, List.copyOf(nations), List.copyOf(settlements), List.copyOf(events));
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
}
