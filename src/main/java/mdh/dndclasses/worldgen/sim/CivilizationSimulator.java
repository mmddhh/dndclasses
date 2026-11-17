package mdh.dndclasses.worldgen.sim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Pure-Java civilization simulation over a real habitability map.
 * <p>
 * The map is produced by {@code mdh.dndclasses.worldgen.mc.RealHabitabilitySampler}; this class
 * has no Minecraft imports and is deterministic for a given map/seed.
 */
public final class CivilizationSimulator {

    private static final int YEARS = 150;
    private static final int CRADLES = 6;
    private static final int MIN_CRADLE_DISTANCE = 10;

    private static final String[] RACES = {"human", "elf", "dwarf", "orc", "halfling", "dragonborn"};
    private static final String[] ROOFS = {"steep", "flat", "domed", "gabled", "spiral"};
    private static final String[] WINDOWS = {"small", "tall", "arched", "round"};
    private static final String[] MOTIFS = {"rune", "vine", "star", "dragon", "hammer", "wave"};
    private static final String[] NAME_START = {"Aer", "Bel", "Cor", "Dun", "Eld", "Fen",
            "Gal", "Hal", "Ith", "Kar", "Lor", "Mor", "Nal", "Or", "Pel", "Quel", "Rav", "Syl",
            "Tor", "Ul"};
    private static final String[] NAME_END = {"dor", "heim", "vale", "mere", "gard", "fell",
            "wood", "hold", "reach", "spire", "moor", "ford", "keep", "hall"};

    private final HabitabilityMap map;
    private final int grid;
    private final int cellSize;
    private final double[][] habitability;
    private final boolean[][] water;
    private final int[][] owner;
    private final List<NationState> nations = new ArrayList<>();

    private CivilizationSimulator(HabitabilityMap map) {
        this.map = map;
        this.grid = map.gridSize();
        this.cellSize = map.cellSize();
        this.habitability = map.habitability();
        this.water = map.water();
        this.owner = new int[grid][grid];
        for (int[] row : owner) {
            java.util.Arrays.fill(row, -1);
        }
    }

    public static SimulationResult run(HabitabilityMap map) {
        return new CivilizationSimulator(map).simulate();
    }

    private SimulationResult simulate() {
        Random rng = new Random(map.seed());
        List<int[]> cradles = pickCradles();
        for (int i = 0; i < cradles.size(); i++) {
            int[] cell = cradles.get(i);
            String race = RACES[i % RACES.length];
            String culture = "roof=" + pick(rng, ROOFS)
                    + ";window=" + pick(rng, WINDOWS)
                    + ";motif=" + pick(rng, MOTIFS);
            NationState nation = new NationState(i, nationName(rng, race), race, culture,
                    cell[0], cell[1]);
            nations.add(nation);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int x = cell[0] + dx;
                    int z = cell[1] + dz;
                    if (inBounds(x, z) && owner[z][x] == -1 && !water[z][x]) {
                        owner[z][x] = i;
                        nation.cells++;
                    }
                }
            }
        }

        List<SimulationResult.Event> events = new ArrayList<>();
        for (int year = 1; year <= YEARS; year++) {
            for (NationState nation : nations) {
                if (nation.cells <= 0) {
                    continue;
                }
                List<int[]> borders = borders(nation.id);
                if (borders.isEmpty()) {
                    continue;
                }
                int[] target = borders.get(rng.nextInt(borders.size()));
                int currentOwner = owner[target[1]][target[0]];
                if (currentOwner == -1) {
                    owner[target[1]][target[0]] = nation.id;
                    nation.cells++;
                    if (year % 12 == 0) {
                        events.add(new SimulationResult.Event(year, "expand", nation.id, -1,
                                nation.name + " 扩张到新的边界。"));
                    }
                } else if (rng.nextDouble() < 0.10D) {
                    NationState other = nations.get(currentOwner);
                    NationState winner = nation.power >= other.power ? nation : other;
                    NationState loser = winner == nation ? other : nation;
                    owner[target[1]][target[0]] = winner.id;
                    winner.cells++;
                    loser.cells--;
                    winner.culture = replaceMotif(winner.culture, motif(loser.culture));
                    if (year % 5 == 0) {
                        events.add(new SimulationResult.Event(year, "war", winner.id, loser.id,
                                winner.name + " 从 " + loser.name + " 手中夺取了一块边境地区。"));
                    }
                }
            }
            for (NationState nation : nations) {
                nation.power = nation.cells * 1.5D + nation.military;
            }
            if (year % 25 == 0) {
                NationState nation = nations.get(rng.nextInt(nations.size()));
                if (nation.cells > 0) {
                    nation.military = Math.max(1, nation.military - 5);
                    events.add(new SimulationResult.Event(year, "plague", nation.id, -1,
                            nation.name + " 爆发瘟疫，军力下降。"));
                }
            }
        }

        List<SimulationResult.Nation> nationRecords = new ArrayList<>();
        for (NationState nation : nations) {
            if (nation.cells <= 0) {
                continue;
            }
            nationRecords.add(new SimulationResult.Nation(nation.id, nation.name, nation.race,
                    nation.culture, nation.cells,
                    nation.capitalX * cellSize + cellSize / 2,
                    nation.capitalZ * cellSize + cellSize / 2));
        }

        List<SimulationResult.Settlement> settlements = new ArrayList<>();
        for (NationState nation : nations) {
            if (nation.cells <= 0) {
                continue;
            }
            List<int[]> owned = ownedCells(nation.id);
            owned.sort(Comparator.comparingDouble((int[] cell) -> -habitability[cell[1]][cell[0]]));
            int count = Math.min(5, Math.max(1, owned.size()));
            for (int i = 0; i < count; i++) {
                int[] cell = owned.get(i);
                String culture = nation.culture + ";trim=" + (i % 2 == 0 ? "carved" : "painted");
                settlements.add(new SimulationResult.Settlement(settlements.size(),
                        settlementName(new Random(map.seed() ^ (cell[0] * 31L) ^ (cell[1] * 131L))),
                        nation.id, cell[0] * cellSize + cellSize / 2,
                        cell[1] * cellSize + cellSize / 2,
                        60 + (int) (habitability[cell[1]][cell[0]] * 400), culture));
            }
        }
        return new SimulationResult(map.seed(), grid, cellSize, List.copyOf(nationRecords),
                List.copyOf(settlements), List.copyOf(events));
    }

    private List<int[]> pickCradles() {
        List<int[]> candidates = new ArrayList<>();
        for (int z = 2; z < grid - 2; z++) {
            for (int x = 2; x < grid - 2; x++) {
                if (water[z][x] || habitability[z][x] < 0.30D) {
                    continue;
                }
                double localBest = habitability[z][x];
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        int nx = x + dx;
                        int nz = z + dz;
                        if (inBounds(nx, nz)) {
                            localBest = Math.max(localBest, habitability[nz][nx]);
                        }
                    }
                }
                if (localBest == habitability[z][x]) {
                    candidates.add(new int[]{x, z});
                }
            }
        }
        candidates.sort(Comparator.comparingDouble((int[] cell) -> -habitability[cell[1]][cell[0]]));
        List<int[]> selected = new ArrayList<>();
        for (int[] candidate : candidates) {
            boolean farEnough = true;
            for (int[] chosen : selected) {
                int dx = chosen[0] - candidate[0];
                int dz = chosen[1] - candidate[1];
                if (dx * dx + dz * dz < MIN_CRADLE_DISTANCE * MIN_CRADLE_DISTANCE) {
                    farEnough = false;
                    break;
                }
            }
            if (farEnough) {
                selected.add(candidate);
                if (selected.size() >= CRADLES) {
                    break;
                }
            }
        }
        return selected;
    }

    private List<int[]> borders(int nation) {
        List<int[]> result = new ArrayList<>();
        for (int z = 1; z < grid - 1; z++) {
            for (int x = 1; x < grid - 1; x++) {
                if (owner[z][x] != nation) {
                    continue;
                }
                for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = x + dir[0];
                    int nz = z + dir[1];
                    if (!inBounds(nx, nz) || water[nz][nx]) {
                        continue;
                    }
                    if (owner[nz][nx] != nation) {
                        result.add(new int[]{nx, nz});
                    }
                }
            }
        }
        return result;
    }

    private List<int[]> ownedCells(int nation) {
        List<int[]> result = new ArrayList<>();
        for (int z = 0; z < grid; z++) {
            for (int x = 0; x < grid; x++) {
                if (owner[z][x] == nation) {
                    result.add(new int[]{x, z});
                }
            }
        }
        return result;
    }

    private boolean inBounds(int x, int z) {
        return x >= 0 && z >= 0 && x < grid && z < grid;
    }

    private static String replaceMotif(String culture, String motif) {
        return culture.replaceAll("motif=[a-z_]+", "motif=" + motif);
    }

    private static String motif(String culture) {
        for (String part : culture.split(";")) {
            if (part.startsWith("motif=")) {
                return part.substring("motif=".length());
            }
        }
        return "rune";
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
        private String culture;
        private final int capitalX;
        private final int capitalZ;
        private int cells;
        private double military = 30;
        private double power = 30;

        private NationState(int id, String name, String race, String culture,
                            int capitalX, int capitalZ) {
            this.id = id;
            this.name = name;
            this.race = race;
            this.culture = culture;
            this.capitalX = capitalX;
            this.capitalZ = capitalZ;
        }
    }
}
