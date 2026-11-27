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
    private record Crust(int[] plate, boolean[] continental, double[] height, int[] age) {}
    private record Frame(int step, Crust crust) {}

    private final Plate[] plates = new Plate[PLATES];
    private final long seed;
    private final Crust initial;

    public TectonicPrototype(long seed) {
        this.seed = seed;
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
        int[] continentStarts = new int[3 + rng.nextInt(3)];
        double[] continentPace = new double[continentStarts.length];
        for (int i = 0; i < continentStarts.length; i++) {
            continentStarts[i] = rng.nextInt(CELLS);
            continentPace[i] = rng.nextDouble(0.52, 1.55);
        }
        int[] grown = grow(continentStarts, continentPace, (int) (CELLS * rng.nextDouble(0.32, 0.42)), 29);
        boolean[] continental = new boolean[CELLS];
        double[] height = new double[CELLS];
        int[] age = new int[CELLS];
        for (int i = 0; i < CELLS; i++) {
            continental[i] = grown[i] >= 0;
            double land = continental[i] ? 1 : 0;
            for (int d = 0; d < DX.length; d++) {
                land += grown[neighbor(i, DX[d], DZ[d])] >= 0 ? 0.13 : 0;
            }
            height[i] = (continental[i] ? 7 + 2 * land : -32 + 18 * land)
                    + (field(i, 29, 61) - 0.5) * 3;
            age[i] = 25 + (int) (field(i, 8, 73) * 115);
        }
        initial = new Crust(owner, continental, height, age);
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

    private Crust advance(Crust previous, int step, int[] events) {
        int[] dx = new int[PLATES], dz = new int[PLATES];
        for (int p = 0; p < PLATES; p++) {
            dx[p] = (int) Math.round(plates[p].vx * step) - (int) Math.round(plates[p].vx * (step - 1));
            dz[p] = (int) Math.round(plates[p].vz * step) - (int) Math.round(plates[p].vz * (step - 1));
        }
        int[] source = new int[CELLS], overlap = new int[CELLS];
        Arrays.fill(source, -1);
        Arrays.fill(overlap, -1);
        for (int i = 0; i < CELLS; i++) {
            int p = previous.plate[i], j = neighbor(i, dx[p], dz[p]);
            int old = source[j];
            if (old < 0) {
                source[j] = i;
            } else if (buoyancy(previous, i) > buoyancy(previous, old)) {
                source[j] = i;
                overlap[j] = old;
            } else if (overlap[j] < 0 || buoyancy(previous, i) > buoyancy(previous, overlap[j])) {
                overlap[j] = i;
            }
        }
        Crust next = new Crust(new int[CELLS], new boolean[CELLS], new double[CELLS], new int[CELLS]);
        Arrays.fill(next.plate, -1);
        var fringe = new ArrayDeque<Integer>();
        double[] strain = new double[CELLS];
        for (int j = 0; j < CELLS; j++) {
            int i = source[j];
            if (i < 0) continue;
            next.plate[j] = previous.plate[i];
            next.continental[j] = previous.continental[i];
            next.age[j] = previous.age[i] + 1;
            next.height[j] = previous.height[i];
            if (!next.continental[j] && next.age[j] < 70) next.height[j] -= 0.17;
            int other = overlap[j];
            if (other >= 0) {
                events[0]++;
                int p = previous.plate[i], q = previous.plate[other];
                // Incident cells span the local contact, independently of plate seed positions.
                double nx = Math.IEEEremainder(other % SIDE - i % SIDE, SIDE);
                double nz = Math.IEEEremainder(other / SIDE - i / SIDE, SIDE);
                double relative = ((plates[p].vx - plates[q].vx) * nx
                        + (plates[p].vz - plates[q].vz) * nz) / Math.hypot(nx, nz);
                double strength = 0.45 + Math.max(0, relative);
                if (next.continental[j] && previous.continental[other]) {
                    strain[j] += 19 * strength;
                    next.height[j] = Math.max(next.height[j], previous.height[other] * 0.72);
                } else {
                    strain[j] += (next.continental[j] ? 12 : 5.2) * strength;
                    int trench = neighbor(j, (int) Math.signum(nx), (int) Math.signum(nz));
                    if (source[trench] >= 0 && !previous.continental[source[trench]]) {
                        strain[trench] -= 8.5 * strength;
                    }
                }
            }
            for (int d = 0; d < DX.length; d++) {
                if (source[neighbor(j, DX[d], DZ[d])] < 0) {
                    fringe.add(j);
                    break;
                }
            }
        }
        while (!fringe.isEmpty()) {
            int i = fringe.remove();
            for (int d = 0; d < DX.length; d++) {
                int j = neighbor(i, DX[d], DZ[d]);
                if (next.plate[j] >= 0) continue;
                next.plate[j] = next.plate[i];
                next.height[j] = -19 + 4 * (field(j, 19, 97) - 0.5);
                events[1]++;
                fringe.add(j);
            }
        }
        double[] broad = new double[CELLS];
        for (int pass = 0; pass < 6; pass++) {
            for (int i = 0; i < CELLS; i++) broad[i] = strain[i] + 0.21 * laplacian(strain, i);
            double[] swap = strain;
            strain = broad;
            broad = swap;
        }
        for (int i = 0; i < CELLS; i++) next.height[i] += strain[i];
        if (step % 2 == 0) erode(next.height);
        return next;
    }

    private static double buoyancy(Crust crust, int i) {
        return (crust.continental[i] ? 1000 : -crust.age[i]) + 0.03 * crust.height[i];
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
        Files.createDirectories(dir);
        int[] events = new int[2];
        Crust crust = initial;
        verifyPlates(initial.plate);
        writeFrame(dir, new Frame(0, crust));
        for (int step = 1; step <= STEPS; step++) {
            crust = advance(crust, step, events);
            if (step % 18 == 0) writeFrame(dir, new Frame(step, crust));
        }
        double[] height = crust.height;
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
            if (channelFlow[i] >= 24 && !water.sea[i] && basin[i] == 0) {
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
            if (channelFlow[i] >= 24) return drop[i] < 0 ? 0xff35c7ee : 0xffad79ed;
            return relief(height, i, false);
        }, false);
        int[] area = new int[PLATES];
        for (int p : initial.plate) area[p]++;
        long continental = java.util.stream.IntStream.range(0, CELLS).filter(i -> initial.continental[i]).count();
        double initialLow = Double.POSITIVE_INFINITY, initialHigh = Double.NEGATIVE_INFINITY;
        double highest = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < CELLS; i++) {
            if (initial.continental[i]) {
                initialLow = Math.min(initialLow, initial.height[i]);
                initialHigh = Math.max(initialHigh, initial.height[i]);
            }
            highest = Math.max(highest, height[i]);
        }
        Files.writeString(dir.resolve("summary.txt"), "seed=" + seed + "\ninitial continental cells=" + continental
                + "\ninitial land elevation range / final peak (above sea)="
                + "%.2f..%.2f / %.2f".formatted(initialLow, initialHigh, highest)
                + "\ncollision / spreading events=" + events[0] + " / " + events[1]
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
    }

    private static int plateColor(Crust crust, int i) {
        if (crust.age[i] < 4) return 0xffd5dde6;
        return Color.HSBtoRGB(crust.plate[i] * 0.618034f % 1, 0.54f, crust.continental[i] ? 0.96f : 0.62f);
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
                      img{display:block;width:100%;height:auto} section{margin-bottom:2rem} select{font:inherit}
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
}
