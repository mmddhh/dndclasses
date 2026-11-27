package mdh.dndclasses.worldgen.terrain;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.SplittableRandom;
import javax.imageio.ImageIO;

/** Offline experiment. Heights are relative to sea level; distances are in coarse cells. */
public final class TectonicPrototype {
    private static final int SIDE = WorldTopology.DEFAULT_SIDE / WorldTopology.DEFAULT_CELL;
    private static final int CELLS = SIDE * SIDE;
    private static final int PLATES = 18;
    private static final int STEPS = 72;
    private static final int[] DX = {1, -1, 0, 0};
    private static final int[] DZ = {0, 0, 1, -1};

    private record Plate(int origin, double vx, double vz) {}
    private record Front(int cell, int label, double cost) {}
    private record Drainage(int[] receiver, int[] order, double[] spill, double[] flow,
                            boolean[] sea, double[] rain) {}
    private record Crust(int[] plate, double[] land, double[] height, double[] age,
                         double[] thickness, double[] stretch, double[] spreading,
                         double[] shares) {}
    private record Frame(int step, Crust crust) {}

    private final Plate[] plates = new Plate[PLATES];
    private final long seed;
    private final double landFraction;
    private final Crust initial;

    public TectonicPrototype(long seed) {
        this(seed, 0.40D);
    }

    public TectonicPrototype(long seed, double landFraction) {
        if (landFraction < 0.10D || landFraction > 0.85D) {
            throw new IllegalArgumentException("landFraction must be between 0.10 and 0.85");
        }
        this.seed = seed;
        this.landFraction = landFraction;
        var rng = new SplittableRandom(seed);
        int[] starts = new int[PLATES];
        double[] pace = new double[PLATES];
        for (int i = 0; i < PLATES; i++) {
            int start;
            boolean tooClose;
            do {
                start = rng.nextInt(CELLS);
                tooClose = false;
                for (int j = 0; j < i; j++) {
                    int dx = Math.abs(start % SIDE - starts[j] % SIDE);
                    int dz = Math.abs(start / SIDE - starts[j] / SIDE);
                    if (Math.min(dx, SIDE - dx) + Math.min(dz, SIDE - dz) < 7) tooClose = true;
                }
            } while (tooClose);
            starts[i] = start;
            double vx = rng.nextDouble(-1, 1), vz = rng.nextDouble(-1, 1);
            double scale = rng.nextDouble(0.32, 0.78) / Math.max(0.001D, Math.hypot(vx, vz));
            plates[i] = new Plate(start, vx * scale, vz * scale);
            pace[i] = (i < 5 ? 0.61 : i < 11 ? 1.30 : 2.15) * rng.nextDouble(0.82, 1.18);
        }
        int[] owner = grow(starts, pace, CELLS, 11);
        int continentCount = landFraction < 0.36D ? 5 : landFraction < 0.56D ? 4 : 3;
        int[] continentStarts = new int[continentCount];
        double[] continentPace = new double[continentStarts.length];
        for (int i = 0; i < continentStarts.length; i++) {
            continentStarts[i] = rng.nextInt(CELLS);
            continentPace[i] = rng.nextDouble(0.52, 1.55);
        }
        int[] grown = grow(continentStarts, continentPace, (int) (CELLS * landFraction), 29);
        double[] continental = new double[CELLS];
        double[] height = new double[CELLS];
        double[] age = new double[CELLS];
        double[] thickness = new double[CELLS];
        for (int i = 0; i < CELLS; i++) {
            continental[i] = grown[i] >= 0 ? 1 : 0;
            double land = continental[i];
            for (int d = 0; d < DX.length; d++) {
                land += grown[neighbor(i, DX[d], DZ[d])] >= 0 ? 0.13 : 0;
            }
            height[i] = (continental[i] > 0 ? 7 + 2 * land : -32 + 18 * land)
                    + (field(i, 29, 61) - 0.5) * 3;
            age[i] = 25 + (int) (field(i, 8, 73) * 115);
            thickness[i] = continental[i];
        }
        double[] shares = new double[CELLS * PLATES];
        for (int i = 0; i < CELLS; i++) shares[i * PLATES + owner[i]] = 1;
        initial = new Crust(owner, continental, height, age, thickness,
                new double[CELLS], new double[CELLS], shares);
    }

    /** Contention on a periodic graph gives each territory a connected growth history. */
    private int[] grow(int[] starts, double[] pace, int limit, int salt) {
        int[] owner = new int[CELLS];
        Arrays.fill(owner, -1);
        var queue = new PriorityQueue<Front>(Comparator.comparingDouble(Front::cost)
                .thenComparingInt(Front::cell).thenComparingInt(Front::label));
        double[] resistance = new double[CELLS];
        for (int i = 0; i < CELLS; i++) {
            resistance[i] = 0.26 + 1.8 * field(i, 10, salt) + 1.2 * field(i, 29, salt + 1);
        }
        for (int i = 0; i < starts.length; i++) queue.add(new Front(starts[i], i, 0));
        for (int n = 0; n < limit && !queue.isEmpty();) {
            Front front = queue.remove();
            int i = front.cell;
            if (owner[i] >= 0) continue;
            owner[i] = front.label;
            n++;
            for (int d = 0; d < DX.length; d++) {
                int j = neighbor(i, DX[d], DZ[d]);
                if (owner[j] < 0) queue.add(new Front(j, front.label,
                        front.cost + pace[front.label] * (resistance[i] + resistance[j]) * 0.5));
            }
        }
        return owner;
    }

    private double field(int i, int period, int salt) {
        double x = (i % SIDE) * (double) period / SIDE;
        double z = (i / SIDE) * (double) period / SIDE;
        int ix = (int) x, iz = (int) z;
        double fx = smooth(0, 1, x - ix), fz = smooth(0, 1, z - iz);
        double a = mix(lattice(ix, iz, period, salt), lattice(ix + 1, iz, period, salt), fx);
        double b = mix(lattice(ix, iz + 1, period, salt), lattice(ix + 1, iz + 1, period, salt), fx);
        return mix(a, b, fz);
    }

    private double lattice(int x, int z, int period, int salt) {
        long h = seed ^ (long) salt * 0x165667B19E3779F9L
                ^ Math.floorMod(x, period) * 0x9E3779B97F4A7C15L
                ^ Math.floorMod(z, period) * 0xC2B2AE3D27D4EB4FL;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return ((h ^ (h >>> 31)) >>> 11) * 0x1.0p-53;
    }

    private Crust advance(Crust previous, double dt, double[] events) {
        Crust next = new Crust(new int[CELLS], new double[CELLS], new double[CELLS], new double[CELLS],
                new double[CELLS], new double[CELLS], new double[CELLS], new double[CELLS * PLATES]);
        double[] cover = new double[CELLS], uplift = new double[CELLS], tension = new double[CELLS];
        for (int i = 0; i < CELLS; i++) {
            int p = previous.plate[i];
            double relief = previous.height[i] - base(previous.land[i], previous.thickness[i], previous.age[i]);
            for (int q = 0; q < PLATES; q++) {
                double share = previous.shares[i * PLATES + q];
                if (share == 0) continue;
                double x = plates[q].vx * dt, z = plates[q].vz * dt;
                int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
                double fx = x - ix, fz = z - iz;
                for (int dz = 0; dz <= 1; dz++) {
                    for (int dx = 0; dx <= 1; dx++) {
                        double w = share * (dx == 0 ? 1 - fx : fx) * (dz == 0 ? 1 - fz : fz);
                        if (w == 0) continue;
                        int j = neighbor(i, ix + dx, iz + dz);
                        next.shares[j * PLATES + q] += w;
                        cover[j] += w;
                        next.land[j] += previous.land[i] * w;
                        next.thickness[j] += previous.thickness[i] * w;
                        next.height[j] += relief * w;
                        next.age[j] += previous.age[i] * w;
                        next.stretch[j] += previous.stretch[i] * previous.land[i] * w;
                    }
                }
            }
            // Relative normal motion across the four faces, smoothed into an extension belt.
            double extension = 0;
            for (int d = 0; d < DX.length; d++) {
                int q = previous.plate[neighbor(i, DX[d], DZ[d])];
                extension += Math.max(0, (plates[q].vx - plates[p].vx) * DX[d]
                        + (plates[q].vz - plates[p].vz) * DZ[d]);
            }
            tension[i] = extension;
        }
        tension = spread(tension, 3);
        for (int i = 0; i < CELLS; i++) {
            double incoming = cover[i];
            double excess = Math.max(0, incoming - 1), gap = Math.max(0, 1 - incoming);
            double landMass = next.land[i];
            double land = Math.min(1, landMass);
            double thickness = next.thickness[i];
            double relief = next.height[i] / Math.max(1, incoming);
            double age = next.age[i] / Math.max(1e-8, incoming) + dt;
            double stretch = landMass > 1e-8 ? next.stretch[i] / landMass : 0;
            double opening = tension[i];
            if (landMass > 1e-8) stretch += opening * dt * 0.37;

            // Extend the existing continental footprint across a gap before inserting ocean floor.
            // The new area receives no additional crust mass, so filling the gap thins the crust.
            double support = landMass / Math.max(1e-8, incoming);
            if (gap > 0 && incoming < 0.4) {
                double nearby = 0;
                for (int d = 0; d < DX.length; d++) {
                    nearby += previous.land[neighbor(i, DX[d], DZ[d])];
                }
                support = mix(nearby * 0.25, support, incoming / 0.4);
            }
            double perLand = thickness / Math.max(1e-8, landMass);
            double rift = smooth(0.48, 1.30, stretch) * smooth(0.75, 0.38, perLand)
                    * smooth(0.035, 0.15, opening);
            double floor = gap * (1 - support * (1 - rift));
            land = Math.min(1, land + gap - floor);

            // Surface oceanic material can subduct in overlap; continental thickness survives it.
            if (excess > 0) {
                events[0] += excess;
                uplift[i] += excess * (land > 0.18 ? 5 + 16 * land : 3.5);
            }
            if (floor > 0) {
                age *= 1 - floor;
                events[1] += floor;
            }
            next.land[i] = land;
            next.thickness[i] = thickness;
            next.stretch[i] = stretch;
            next.spreading[i] = floor / dt;
            next.age[i] = age;
            next.height[i] = base(land, thickness, age) + relief;
            int incumbent = previous.plate[i], winner = incumbent;
            double best = next.shares[i * PLATES + incumbent];
            for (int p = 0; p < PLATES; p++) {
                double vote = next.shares[i * PLATES + p];
                if (vote > best) {
                    winner = p;
                    best = vote;
                }
            }
            next.plate[i] = winner;
            double retained = 0;
            for (int p = 0; p < PLATES; p++) {
                int k = i * PLATES + p;
                double share = next.shares[k] / Math.max(1e-8, incoming);
                next.shares[k] = share >= 0.0001 ? share : 0;
                retained += next.shares[k];
            }
            if (retained == 0) next.shares[i * PLATES + winner] = 1;
            else for (int p = 0; p < PLATES; p++) next.shares[i * PLATES + p] /= retained;
        }
        double[] belt = spread(uplift, 5);
        for (int i = 0; i < CELLS; i++) next.height[i] += belt[i];
        return next;
    }

    private static double base(double land, double thickness, double age) {
        double continental = clamp(10 + 47 * (thickness / Math.max(1e-8, land) - 1), -32, 84);
        double oceanic = -18 - Math.min(14, age * 0.20);
        return mix(oceanic, continental, land);
    }

    private static double[] spread(double[] field, int passes) {
        double[] tmp = new double[CELLS];
        for (int pass = 0; pass < passes; pass++) {
            for (int i = 0; i < CELLS; i++) tmp[i] = field[i] + 0.18 * laplacian(field, i);
            double[] swap = tmp;
            tmp = field;
            field = swap;
        }
        return field;
    }

    private void erode(double[] height) {
        Drainage water = drain(height);
        double[] change = new double[CELLS];
        for (int i : water.order) {
            int j = water.receiver[i];
            change[i] += 0.065 * laplacian(height, i);
            if (j < 0) continue;
            double slope = Math.max(0, height[i] - height[j]);
            double cut = Math.min(slope * 0.14, 0.015 * Math.sqrt(water.flow[i]) * slope);
            change[i] -= cut;
            change[j] += cut * (slope < 3 || water.sea[j] ? 0.55 : 0.25);
        }
        for (int i = 0; i < CELLS; i++) height[i] += change[i];
    }

    /** Largest contiguous below-sea region is the ocean; isolated depressions have outlets to test. */
    private static boolean[] ocean(double[] height) {
        int[] region = new int[CELLS];
        Arrays.fill(region, -1);
        int largest = -2, biggest = 0;
        var queue = new ArrayDeque<Integer>();
        for (int start = 0; start < CELLS; start++) {
            if (region[start] >= 0 || height[start] > 0) continue;
            int count = 0;
            region[start] = start;
            queue.add(start);
            while (!queue.isEmpty()) {
                int i = queue.remove();
                count++;
                for (int d = 0; d < DX.length; d++) {
                    int j = neighbor(i, DX[d], DZ[d]);
                    if (region[j] < 0 && height[j] <= 0) {
                        region[j] = start;
                        queue.add(j);
                    }
                }
            }
            if (count > biggest) {
                biggest = count;
                largest = start;
            }
        }
        boolean[] sea = new boolean[CELLS];
        for (int i = 0; i < CELLS; i++) sea[i] = region[i] == largest;
        return sea;
    }

    private Drainage drain(double[] height) {
        boolean[] sea = ocean(height);
        double[] rain = new double[CELLS];
        double[] spill = height.clone();
        var queue = new PriorityQueue<>(Comparator.<Integer>comparingDouble(i -> spill[i]).thenComparingInt(i -> i));
        int[] parent = new int[CELLS], order = new int[CELLS];
        Arrays.fill(parent, -2);
        for (int i = 0; i < CELLS; i++) {
            int upwind = neighbor(i, -5, 0);
            rain[i] = sea[i] ? 0 : clamp(0.20 + 0.74 * field(i, 7, 481)
                    + clamp((height[i] - height[upwind]) * 0.009, -0.22, 0.28), 0.05, 1);
            if (sea[i]) {
                parent[i] = -1;
                spill[i] = 0;
                queue.add(i);
            }
        }
        if (queue.isEmpty()) throw new IllegalStateException("No ocean outlet");
        int rank = 0;
        int[] position = new int[CELLS];
        while (!queue.isEmpty()) {
            int i = queue.remove();
            position[i] = rank;
            order[rank++] = i;
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dz == 0) continue;
                    int j = neighbor(i, dx, dz);
                    if (parent[j] != -2) continue;
                    parent[j] = i;
                    spill[j] = Math.max(height[j], spill[i]);
                    queue.add(j);
                }
            }
        }
        // On graded ground follow the steepest eligible descent; retain flood parents on flats.
        for (int i : order) {
            if (sea[i]) continue;
            double grade = 0;
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dz == 0) continue;
                    int j = neighbor(i, dx, dz);
                    if (position[j] >= position[i] || spill[j] > spill[i]) continue;
                    double slope = (height[i] - (sea[j] ? 0 : height[j])) / (dx != 0 && dz != 0 ? 1.4142 : 1);
                    if (slope > grade) {
                        parent[i] = j;
                        grade = slope;
                    }
                }
            }
        }
        double[] flow = rain.clone();
        for (int k = CELLS - 1; k >= 0; k--) {
            int i = order[k];
            if (parent[i] >= 0) flow[parent[i]] += flow[i];
        }
        return new Drainage(parent, order, spill, flow, sea, rain);
    }

    /** Classify each depression using its inflow, local humidity and evaporation. */
    private byte[] basins(double[] height, Drainage water) {
        byte[] kind = new byte[CELLS];
        var pending = new ArrayDeque<Integer>();
        var members = new ArrayDeque<Integer>();
        for (int i : water.order) {
            if (kind[i] != 0 || water.sea[i] || water.spill[i] - height[i] < 1) continue;
            double level = water.spill[i];
            double supply = 0, loss = 0, localRain = 0;
            kind[i] = -1;
            pending.add(i);
            while (!pending.isEmpty()) {
                int j = pending.remove();
                members.add(j);
                localRain += water.rain[j];
                loss += 0.65 + 1.05 * field(j, 6, 294);
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dz == 0) continue;
                        int k = neighbor(j, dx, dz);
                        if (water.spill[k] == level && !water.sea[k] && height[k] <= level - 1) {
                            if (kind[k] == 0) {
                                kind[k] = -1;
                                pending.add(k);
                            }
                        } else if (water.receiver[k] == j) {
                            supply += water.flow[k];
                        }
                    }
                }
            }
            double balance = (localRain + supply) / loss;
            byte type = (byte) (balance >= 1.1 ? 3 : balance >= 0.68 ? 2 : 1);
            while (!members.isEmpty()) kind[members.remove()] = type;
        }
        return kind;
    }

    private void export(Path dir, long seed) throws IOException {
        export(dir, seed, 1, false);
    }

    private void export(Path dir, long seed, int substeps, boolean traceRift) throws IOException {
        Files.createDirectories(dir);
        double[] events = new double[2];
        Crust crust = initial;
        verifyPlates(initial.plate);
        writeFrame(dir, new Frame(0, crust));
        StringBuilder trace = new StringBuilder(
                "step,baseline_land_connected_to_ocean,low_land_not_connected_to_ocean,small_plate_fragments\n");
        boolean[] baselineSea = null;
        for (int step = 1; step <= STEPS; step++) {
            for (int sub = 0; sub < substeps; sub++) crust = advance(crust, 1.0 / substeps, events);
            if (step % 2 == 0) erode(crust.height);
            if (step % 18 == 0 || (traceRift && step >= 18 && step <= 36)) {
                writeFrame(dir, new Frame(step, crust));
            }
            if (traceRift && step >= 18 && step <= 36) {
                boolean[] sea = ocean(crust.height);
                if (baselineSea == null) baselineSea = sea;
                int flooded = 0, isolated = 0;
                // Lower-left landmass in seed 7, measured in the original preview coordinates.
                for (int z = 155; z < 225; z++) {
                    for (int x = 35; x < 110; x++) {
                        int i = z * SIDE + x;
                        if (!baselineSea[i] && sea[i]) flooded++;
                        if (!baselineSea[i] && !sea[i] && crust.height[i] <= 0) isolated++;
                    }
                }
                trace.append(step).append(',').append(flooded).append(',').append(isolated).append(',')
                        .append(smallFragments(crust.plate)).append('\n');
            }
        }
        if (traceRift) Files.writeString(dir.resolve("rift-trace.csv"), trace);
        double[] height = crust.height;
        double startingMass = Arrays.stream(initial.thickness).sum();
        double endingMass = Arrays.stream(crust.thickness).sum();
        if (!Double.isFinite(endingMass) || Math.abs(startingMass - endingMass) > startingMass * 1e-8) {
            throw new IllegalStateException("Continental mass drift: " + startingMass + " -> " + endingMass);
        }
        Drainage water = drain(height);
        byte[] basin = basins(height, water);
        verify(water);
        double[] channelFlow = water.rain.clone();
        for (int k = CELLS - 1; k >= 0; k--) {
            int i = water.order[k], j = water.receiver[i];
            if (j >= 0 && basin[i] != 1 && basin[i] != 2) channelFlow[j] += channelFlow[i];
        }
        int[] drop = new int[CELLS];
        int seaRoutes = 0, basinRoutes = 0, longest = 0;
        int[] basinCells = new int[4];
        int[] steps = new int[CELLS];
        for (int i : water.order) {
            basinCells[basin[i]]++;
            int j = water.receiver[i];
            drop[i] = basin[i] == 1 || basin[i] == 2 ? i : j < 0 ? -1 : drop[j];
            if (j >= 0 && basin[i] != 1 && basin[i] != 2) steps[i] = steps[j] + 1;
            if (channelFlow[i] >= 8 && !water.sea[i] && basin[i] == 0) {
                if (drop[i] < 0) seaRoutes++; else basinRoutes++;
                longest = Math.max(longest, steps[i]);
            }
        }
        write(dir, "plates", i -> plateColor(initial, i), true);
        write(dir, "initial", i -> relief(initial.height, i, initial.height[i] <= 0), false);
        write(dir, "evolved", i -> relief(height, i, water.sea[i]), false);
        write(dir, "drainage", i -> {
            if (water.sea[i]) return relief(height, i, true);
            if (basin[i] == 3) return 0xff246bd6;
            if (basin[i] == 2) return 0xff9377ce;
            if (basin[i] == 1) return 0xffe1c585;
            if (channelFlow[i] >= 8) return drop[i] < 0 ? 0xff35c7ee : 0xffad79ed;
            return relief(height, i, false);
        }, false);
        int[] area = new int[PLATES];
        for (int p : initial.plate) area[p]++;
        long continental = java.util.stream.IntStream.range(0, CELLS).filter(i -> initial.land[i] > 0.5).count();
        int islands = countIslands(initial.land);
        double initialLow = Double.POSITIVE_INFINITY, initialHigh = Double.NEGATIVE_INFINITY;
        double highest = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < CELLS; i++) {
            if (initial.land[i] > 0.5) {
                initialLow = Math.min(initialLow, initial.height[i]);
                initialHigh = Math.max(initialHigh, initial.height[i]);
            }
            highest = Math.max(highest, height[i]);
        }
        Files.writeString(dir.resolve("summary.txt"), "seed=" + seed + "\nrequested land fraction=" + landFraction
                + "\ninitial continental cells=" + continental + "\ninitial land masses=" + islands
                + "\ninitial land elevation range / final peak (above sea)="
                + "%.2f..%.2f / %.2f".formatted(initialLow, initialHigh, highest)
                + "\nsubsteps per iteration=" + substeps
                + "\ncontinental mass (initial / final)=" + startingMass + " / " + endingMass
                + "\ncollision / spreading area (cumulative grid cells)=" + events[0] + " / " + events[1]
                + "\nfinal small plate fragments (<16 cells)=" + smallFragments(crust.plate)
                + "\ninitial plate area and velocities (grid x,z):\n"
                + java.util.stream.IntStream.range(0, PLATES)
                    .mapToObj(i -> i + ": " + area[i] + " cells, " + plates[i].vx + ", " + plates[i].vz + "\n")
                    .collect(java.util.stream.Collectors.joining())
                + "sea-bound channel cells=" + seaRoutes + "\ninterior-bound channel cells=" + basinRoutes
                + "\ndry / seasonal / overflowing basin cells=" + basinCells[1]
                + " / " + basinCells[2] + " / " + basinCells[3]
                + "\nlongest channel-to-outlet route (grid steps)=" + longest + "\n");
    }

    private void verify(Drainage water) {
        int[] rank = new int[CELLS];
        for (int i = 0; i < CELLS; i++) rank[water.order[i]] = i;
        for (int i = 0; i < CELLS; i++) {
            int j = water.receiver[i];
            if ((j < 0 && !water.sea[i]) || (j >= 0 && (rank[j] >= rank[i] || water.spill[j] > water.spill[i]))) {
                throw new IllegalStateException("Invalid drain at cell " + i);
            }
        }
    }

    private void verifyPlates(int[] owner) {
        boolean[] visited = new boolean[CELLS];
        var pending = new ArrayDeque<Integer>();
        for (int p = 0; p < PLATES; p++) {
            int first = -1, area = 0;
            for (int i = 0; i < CELLS; i++) {
                if (owner[i] == p) {
                    first = i;
                    area++;
                }
            }
            if (first < 0) throw new IllegalStateException("Empty plate " + p);
            pending.add(first);
            visited[first] = true;
            while (!pending.isEmpty()) {
                int i = pending.remove();
                area--;
                for (int d = 0; d < DX.length; d++) {
                    int j = neighbor(i, DX[d], DZ[d]);
                    if (!visited[j] && owner[j] == p) {
                        visited[j] = true;
                        pending.add(j);
                    }
                }
            }
            if (area != 0) throw new IllegalStateException("Disconnected plate " + p);
        }
    }

    private void writeFrame(Path dir, Frame frame) throws IOException {
        boolean[] sea = ocean(frame.crust.height);
        String name = "step-%02d".formatted(frame.step);
        write(dir, name, i -> relief(frame.crust.height, i, sea[i]), false);
        write(dir, name + "-plates", i -> plateColor(frame.crust, i), false);
        write(dir, name + "-thickness", i -> {
            double land = frame.crust.land[i];
            double h = clamp(frame.crust.thickness[i] / Math.max(1e-8, land), 0, 1.5)
                    * smooth(0.06, 0.55, land);
            return rgb((int) (52 + h * 116), (int) (80 + h * 94), (int) (112 + h * 40));
        }, false);
        write(dir, name + "-rift", i -> {
            double t = Math.min(1, frame.crust.stretch[i]);
            double s = Math.min(1, frame.crust.spreading[i] * 2);
            return rgb((int) (24 + t * 225), (int) (30 + s * 154), (int) (40 + s * 210));
        }, false);
    }

    private static int plateColor(Crust crust, int i) {
        return Color.HSBtoRGB(crust.plate[i] * 0.618034f % 1, 0.60f, 0.90f);
    }

    private static int smallFragments(int[] owner) {
        boolean[] visited = new boolean[CELLS];
        var queue = new ArrayDeque<Integer>();
        int count = 0;
        for (int first = 0; first < CELLS; first++) {
            if (visited[first]) continue;
            int area = 0;
            queue.add(first);
            visited[first] = true;
            while (!queue.isEmpty()) {
                int i = queue.remove();
                area++;
                for (int d = 0; d < DX.length; d++) {
                    int j = neighbor(i, DX[d], DZ[d]);
                    if (!visited[j] && owner[j] == owner[first]) {
                        visited[j] = true;
                        queue.add(j);
                    }
                }
            }
            if (area < 16) count++;
        }
        return count;
    }

    private static int countIslands(double[] land) {
        boolean[] visited = new boolean[CELLS];
        var queue = new ArrayDeque<Integer>();
        int count = 0;
        for (int first = 0; first < CELLS; first++) {
            if (visited[first] || land[first] <= 0.5) continue;
            count++;
            visited[first] = true;
            queue.add(first);
            while (!queue.isEmpty()) {
                int i = queue.remove();
                for (int d = 0; d < DX.length; d++) {
                    int j = neighbor(i, DX[d], DZ[d]);
                    if (!visited[j] && land[j] > 0.5) {
                        visited[j] = true;
                        queue.add(j);
                    }
                }
            }
        }
        return count;
    }

    private void write(Path dir, String name, java.util.function.IntUnaryOperator color, boolean arrows)
            throws IOException {
        int zoom = 4;
        var image = new BufferedImage(SIDE * zoom, SIDE * zoom, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        try {
            for (int i = 0; i < CELLS; i++) {
                g.setColor(new Color(color.applyAsInt(i)));
                g.fillRect(i % SIDE * zoom, i / SIDE * zoom, zoom, zoom);
            }
            if (arrows) {
                g.setColor(Color.WHITE);
                for (Plate plate : plates) {
                    double x = plate.origin % SIDE * zoom, z = plate.origin / SIDE * zoom;
                    int tx = (int) (x + plate.vx * 27), tz = (int) (z + plate.vz * 27);
                    g.drawLine((int) x, (int) z, tx, tz);
                    double len = Math.hypot(plate.vx, plate.vz);
                    for (int wing : new int[]{-1, 1}) {
                        g.drawLine(tx, tz, tx - (int) (7 * plate.vx / len + wing * 4 * plate.vz / len),
                                tz - (int) (7 * plate.vz / len - wing * 4 * plate.vx / len));
                    }
                }
            }
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", dir.resolve(name + ".png").toFile());
    }

    private static int relief(double[] height, int i, boolean sea) {
        double h = height[i];
        int light = (int) clamp((height[neighbor(i, -1, -1)] - height[neighbor(i, 1, 1)]) * 1.9, -34, 34);
        if (sea) return rgb(30 + light / 4, (int) (111 + h) + light / 3, (int) (160 + h) + light / 3);
        double rock = smooth(8, 56, h), snow = smooth(60, 110, h);
        return rgb((int) mix(mix(78, 158, rock), 234, snow) + light,
                (int) mix(mix(126, 139, rock), 236, snow) + light,
                (int) mix(mix(80, 123, rock), 239, snow) + light);
    }

    private static double laplacian(double[] field, int i) {
        double sum = 0;
        for (int d = 0; d < DX.length; d++) sum += field[neighbor(i, DX[d], DZ[d])] - field[i];
        return sum;
    }

    private static int neighbor(int i, int dx, int dz) {
        return Math.floorMod(i / SIDE + dz, SIDE) * SIDE + Math.floorMod(i % SIDE + dx, SIDE);
    }

    private static int rgb(int r, int g, int b) {
        return 0xff000000 | (int) clamp(r, 0, 255) << 16
                | (int) clamp(g, 0, 255) << 8 | (int) clamp(b, 0, 255);
    }

    private static double mix(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double smooth(double low, double high, double x) {
        double t = clamp((x - low) / (high - low), 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 2 && args[0].equals("--check-rift")) {
            Path dir = Path.of(args[1]);
            var prototype = new TectonicPrototype(7);
            prototype.export(dir.resolve("full-step"), 7, 1, true);
            prototype.export(dir.resolve("half-step"), 7, 2, true);
            return;
        }
        if (args.length >= 3 && args[0].equals("--ratios")) {
            Path dir = Path.of(args[1]);
            double[] ratios = {0.30D, 0.40D, 0.50D, 0.60D, 0.70D};
            String[] labels = {"3-7", "4-6", "5-5", "6-4", "7-3"};
            StringBuilder cards = new StringBuilder();
            for (int r = 0; r < ratios.length; r++) {
                cards.append("<section><h2>陆地 : 海洋 = ").append(labels[r]).append("</h2>");
                for (int i = 2; i < args.length; i++) {
                    long seed = Long.parseLong(args[i]);
                    Path target = dir.resolve(labels[r]).resolve(Long.toString(seed));
                    new TectonicPrototype(seed, ratios[r]).export(target, seed);
                    String relative = labels[r] + "/" + seed;
                    cards.append("<article><h3>种子 ").append(seed).append("</h3><div class='maps'>")
                            .append("<a href='").append(relative).append("/evolved.png'><img src='")
                            .append(relative).append("/evolved.png' alt='地形'></a>")
                            .append("<a href='").append(relative).append("/plates.png'><img src='")
                            .append(relative).append("/plates.png' alt='板块'></a>")
                            .append("<a href='").append(relative).append("/drainage.png'><img src='")
                            .append(relative).append("/drainage.png' alt='水文'></a></div>")
                            .append("<a href='").append(relative).append("/summary.txt'>统计</a></article>");
                }
                cards.append("</section>");
            }
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("index.html"), galleryHtml("板块地形：海陆比例对照", cards.toString()));
            return;
        }
        if (args.length >= 3 && args[0].equals("--gallery")) {
            Path dir = Path.of(args[1]);
            StringBuilder cards = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                long seed = Long.parseLong(args[i]);
                new TectonicPrototype(seed).export(dir.resolve(Long.toString(seed)), seed);
                cards.append("""
                        <section><h2>Seed %1$d · <a href="%1$d/summary.txt">statistics</a></h2>
                        <div class="maps">
                          <a href="%1$d/step-72.png" data-frame="%1$d/step-%%s.png">
                            <img src="%1$d/step-72.png" alt="Terrain"><span>Terrain</span></a>
                          <a href="%1$d/step-72-plates.png" data-frame="%1$d/step-%%s-plates.png">
                            <img src="%1$d/step-72-plates.png" alt="Plates and crust"><span>Plates and crust</span></a>
                          <a href="%1$d/drainage.png">
                            <img src="%1$d/drainage.png" alt="Final drainage"><span>Final drainage</span></a>
                        </div></section>
                        """.formatted(seed));
            }
            Files.writeString(dir.resolve("index.html"), """
                    <!doctype html><html lang="en"><meta charset="utf-8">
                    <meta name="viewport" content="width=device-width,initial-scale=1">
                    <title>Tectonic terrain comparison</title>
                    <style>
                      body{margin:2rem auto;padding:0 1rem;max-width:80rem;background:#17232a;color:#edf3f5;font:1rem system-ui}
                      a{color:#9ed4f3} h2 a{font-size:1rem} .maps{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:1rem}
                      img{display:block;width:100%;height:auto;image-rendering:pixelated} section{margin-bottom:2rem} select{font:inherit}
                    </style>
                    <h1>Tectonic terrain comparison</h1>
                    <label>Iteration <select id="step"><option>00</option><option>18</option><option>36</option>
                    <option>54</option><option selected>72</option></select></label>
                    <p>Change the iteration to compare land and plate motion. The drainage map always shows the final state.</p>
                    """ + cards + """
                    <script>
                    document.getElementById('step').addEventListener('change', event => {
                      document.querySelectorAll('[data-frame]').forEach(link => {
                        const path = link.dataset.frame.replace('%s', event.target.value);
                        link.setAttribute('href', path);
                        link.querySelector('img').setAttribute('src', path);
                      });
                    });
                    </script></html>
                    """);
            return;
        }
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 12345L;
        new TectonicPrototype(seed).export(Path.of(args.length > 1 ? args[1] : "build/tectonic-preview"), seed);
    }

    private static String galleryHtml(String title, String cards) {
        return "<!doctype html><html lang='zh-CN'><meta charset='utf-8'><title>" + title + "</title>"
                + "<style>body{margin:2rem auto;padding:0 1rem;max-width:100rem;background:#17232a;color:#edf3f5;font:1rem system-ui}"
                + "a{color:#9ed4f3}section{margin:2rem 0;padding:1rem;border:1px solid #49606c}"
                + ".maps{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:.5rem}"
                + "article{display:inline-block;vertical-align:top;width:min(31%,30rem);margin:.5rem}"
                + "img{display:block;width:100%;height:auto;image-rendering:pixelated}"
                + ".legend{display:flex;flex-wrap:wrap;gap:1rem}.swatch{display:inline-flex;gap:.3rem;align-items:center}"
                + ".swatch i{width:1rem;height:1rem;border:1px solid #789}.sea{background:#236ea3}.land{background:#609653}"
                + ".hill{background:#a9784c}.mountain{background:#f4f1e9}.plate{background:linear-gradient(90deg,#d84f83,#55b8aa,#7d63cc)}"
                + ".river{background:#34c7ee}.lake{background:#ad79ed}</style><h1>" + title + "</h1>"
                + "<p>每组按海陆比例分栏；每个种子依次显示地形、板块和最终水文。点击图片查看原尺寸。</p>"
                + "<div class='legend'><span class='swatch'><i class='sea'></i>海洋</span><span class='swatch'><i class='land'></i>低地</span>"
                + "<span class='swatch'><i class='hill'></i>高地</span><span class='swatch'><i class='mountain'></i>高山</span>"
                + "<span class='swatch'><i class='plate'></i>板块编号</span><span class='swatch'><i class='river'></i>入海河流</span>"
                + "<span class='swatch'><i class='lake'></i>内陆水系</span></div>" + cards + "</html>";
    }
}
