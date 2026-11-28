package mdh.dndclasses.worldgen.terrain;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
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
    private final int side;
    private final int cells;
    private final int resolution;
    private static final int PLATES = 18;
    private static final int STEPS = 72;
    private static final Color SEA_CHANNEL = new Color(0x35c7ee);
    private static final Color INLAND_CHANNEL = new Color(0xad79ed);
    private static final int[] DX = {1, -1, 0, 0};
    private static final int[] DZ = {0, 0, 1, -1};

    private record Plate(int origin, double vx, double vz) {}
    private record Front(int cell, int label, double cost) {}
    private record Drainage(int[] receiver, int[] order, double[] spill, double[] flow,
                            boolean[] sea, double[] rain) {}
    private record Crust(int[] plate, double[] land, double[] height, double[] age,
                         double[] thickness, double[] stretch, double[] spreading,
                         Shares shares) {}
    private record Frame(int step, Crust crust) {}

    // Fine grids only allocate a plate's shares where its material has arrived.
    private static final class Shares {
        private static final int BLOCK_SHIFT = 6;
        private static final int BLOCK_SIZE = 1 << BLOCK_SHIFT;
        private final int[] initialOwner;
        private final double[] dense;
        private final double[][] blocks;
        private final int[] occupied;

        Shares(int[] initialOwner) {
            this.initialOwner = initialOwner;
            dense = null;
            blocks = null;
            occupied = null;
        }

        Shares(int cells, boolean sparse) {
            initialOwner = null;
            dense = sparse ? null : new double[cells * PLATES];
            int blockCount = (cells + BLOCK_SIZE - 1) >> BLOCK_SHIFT;
            blocks = sparse ? new double[PLATES * blockCount][] : null;
            occupied = sparse ? new int[blockCount] : null;
        }

        int candidates(int cell) {
            if (initialOwner != null) return 1 << initialOwner[cell];
            return occupied == null ? (1 << PLATES) - 1 : occupied[cell >> BLOCK_SHIFT];
        }

        double get(int cell, int plate) {
            if (initialOwner != null) return initialOwner[cell] == plate ? 1 : 0;
            if (dense != null) return dense[cell * PLATES + plate];
            double[] block = blocks[(cell >> BLOCK_SHIFT) * PLATES + plate];
            return block == null ? 0 : block[cell & (BLOCK_SIZE - 1)];
        }

        void set(int cell, int plate, double value) {
            if (dense != null) {
                dense[cell * PLATES + plate] = value;
                return;
            }
            int index = (cell >> BLOCK_SHIFT) * PLATES + plate;
            double[] block = blocks[index];
            if (block == null) {
                if (value == 0) return;
                blocks[index] = block = new double[BLOCK_SIZE];
                occupied[cell >> BLOCK_SHIFT] |= 1 << plate;
            }
            block[cell & (BLOCK_SIZE - 1)] = value;
        }
    }

    private final Plate[] plates = new Plate[PLATES];
    private final long seed;
    private final double landFraction;
    private final Crust initial;

    public TectonicPrototype(long seed) {
        this(seed, 0.40D);
    }

    public TectonicPrototype(long seed, double landFraction) {
        this(seed, landFraction, 1);
    }

    private TectonicPrototype(long seed, double landFraction, int resolution) {
        this.resolution = resolution;
        this.side = resolution * WorldTopology.DEFAULT_SIDE / WorldTopology.DEFAULT_CELL;
        this.cells = side * side;
        if (!(landFraction >= 0.10D && landFraction <= 0.85D)) {
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
                start = seedCell(rng);
                tooClose = false;
                for (int j = 0; j < i; j++) {
                    int dx = Math.abs(start % side - starts[j] % side);
                    int dz = Math.abs(start / side - starts[j] / side);
                    if (Math.min(dx, side - dx) + Math.min(dz, side - dz) < 7 * resolution) tooClose = true;
                }
            } while (tooClose);
            starts[i] = start;
            double vx = rng.nextDouble(-1, 1), vz = rng.nextDouble(-1, 1);
            double scale = rng.nextDouble(0.32, 0.78) / Math.max(0.001D, Math.hypot(vx, vz));
            plates[i] = new Plate(start, vx * scale, vz * scale);
            pace[i] = (i < 5 ? 0.61 : i < 11 ? 1.30 : 2.15) * rng.nextDouble(0.82, 1.18);
        }
        int[] owner = grow(starts, pace, cells, 11);
        int continentCount = landFraction < 0.36D ? 5 : landFraction < 0.56D ? 4 : 3;
        int[] continentStarts = new int[continentCount];
        double[] continentPace = new double[continentStarts.length];
        for (int i = 0; i < continentStarts.length; i++) {
            continentStarts[i] = seedCell(rng);
            continentPace[i] = rng.nextDouble(0.52, 1.55);
        }
        int mainLand = (int) (cells * landFraction * (landFraction <= 0.4D ? 0.72D : 0.82D));
        int[] grown = grow(continentStarts, continentPace, mainLand, 29);
        boolean[] occupied = new boolean[cells];
        for (int i = 0; i < cells; i++) occupied[i] = grown[i] >= 0;
        int islandBudget = (int) (cells * landFraction) - mainLand;
        int islandCount = Math.max(8, (int) Math.round(landFraction * 38));
        int[] islandStarts = new int[islandCount];
        int selected = 0;
        int attempts = 0;
        while (selected < islandCount && attempts++ < islandCount * 200) {
            int candidate = seedCell(rng);
            if (occupied[candidate]) continue;
            boolean far = true;
            for (int j = 0; j < selected; j++) {
                int dx = Math.abs(candidate % side - islandStarts[j] % side);
                int dz = Math.abs(candidate / side - islandStarts[j] / side);
                if (Math.min(dx, side - dx) + Math.min(dz, side - dz) < 12 * resolution) {
                    far = false;
                    break;
                }
            }
            if (far) islandStarts[selected++] = candidate;
        }
        if (selected < islandStarts.length) islandStarts = Arrays.copyOf(islandStarts, selected);
        int[] islands = growIslands(occupied, islandStarts, islandBudget, 67);
        for (int i = 0; i < cells; i++) if (islands[i] >= 0) grown[i] = islands[i];
        double[] continental = new double[cells];
        double[] height = new double[cells];
        double[] age = new double[cells];
        double[] thickness = new double[cells];
        for (int i = 0; i < cells; i++) {
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
        initial = new Crust(owner, continental, height, age, thickness,
                new double[cells], new double[cells], new Shares(owner));
    }

    /** Contention on a periodic graph gives each territory a connected growth history. */
    private int[] grow(int[] starts, double[] pace, int limit, int salt) {
        int[] owner = new int[cells];
        Arrays.fill(owner, -1);
        var queue = new PriorityQueue<Front>(Comparator.comparingDouble(Front::cost)
                .thenComparingInt(Front::cell).thenComparingInt(Front::label));
        double[] resistance = new double[cells];
        for (int i = 0; i < cells; i++) {
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

    /** Grow separated island bodies only through ocean cells, preserving the mainland mask. */
    private int[] growIslands(boolean[] occupied, int[] starts, int limit, int salt) {
        int[] owner = new int[cells];
        Arrays.fill(owner, -1);
        var queue = new PriorityQueue<Front>(Comparator.comparingDouble(Front::cost)
                .thenComparingInt(Front::cell).thenComparingInt(Front::label));
        for (int i = 0; i < starts.length; i++) {
            if (!occupied[starts[i]]) queue.add(new Front(starts[i], i, 0));
        }
        int claimed = 0;
        while (claimed < limit && !queue.isEmpty()) {
            Front front = queue.remove();
            int i = front.cell;
            if (occupied[i] || owner[i] >= 0) continue;
            owner[i] = front.label;
            claimed++;
            for (int d = 0; d < DX.length; d++) {
                int j = neighbor(i, DX[d], DZ[d]);
                if (!occupied[j] && owner[j] < 0) {
                    double irregular = 0.55D + 1.1D * field(j, 13, salt + front.label * 17);
                    queue.add(new Front(j, front.label, front.cost + irregular));
                }
            }
        }
        return owner;
    }

    private int seedCell(SplittableRandom rng) {
        int coarseSide = side / resolution;
        int cell = rng.nextInt(coarseSide * coarseSide);
        return (cell / coarseSide * resolution + resolution / 2) * side
                + cell % coarseSide * resolution + resolution / 2;
    }

    private double field(int i, int period, int salt) {
        double x = (i % side) * (double) period / side;
        double z = (i / side) * (double) period / side;
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
        Crust next = new Crust(new int[cells], new double[cells], new double[cells], new double[cells],
                new double[cells], new double[cells], new double[cells], new Shares(cells, resolution >= 8));
        double[] cover = new double[cells], uplift = new double[cells], tension = new double[cells];
        for (int i = 0; i < cells; i++) {
            int p = previous.plate[i];
            double relief = previous.height[i] - base(previous.land[i], previous.thickness[i], previous.age[i]);
            for (int candidates = previous.shares.candidates(i); candidates != 0; candidates &= candidates - 1) {
                int q = Integer.numberOfTrailingZeros(candidates);
                double share = previous.shares.get(i, q);
                if (share == 0) continue;
                double x = plates[q].vx * dt * resolution, z = plates[q].vz * dt * resolution;
                int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
                double fx = x - ix, fz = z - iz;
                for (int dz = 0; dz <= 1; dz++) {
                    for (int dx = 0; dx <= 1; dx++) {
                        double w = share * (dx == 0 ? 1 - fx : fx) * (dz == 0 ? 1 - fz : fz);
                        if (w == 0) continue;
                        int j = neighbor(i, ix + dx, iz + dz);
                        next.shares.set(j, q, next.shares.get(j, q) + w);
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
        tension = spread(tension, 3 * resolution * resolution);
        for (int i = 0; i < cells; i++) {
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
            double best = next.shares.get(i, incumbent);
            int material = next.shares.candidates(i);
            for (int candidates = material; candidates != 0; candidates &= candidates - 1) {
                int p = Integer.numberOfTrailingZeros(candidates);
                double vote = next.shares.get(i, p);
                if (vote > best) {
                    winner = p;
                    best = vote;
                }
            }
            next.plate[i] = winner;
            double retained = 0;
            for (int candidates = material; candidates != 0; candidates &= candidates - 1) {
                int p = Integer.numberOfTrailingZeros(candidates);
                double share = next.shares.get(i, p) / Math.max(1e-8, incoming);
                share = share >= 0.0001 ? share : 0;
                next.shares.set(i, p, share);
                retained += share;
            }
            if (retained == 0) next.shares.set(i, winner, 1);
            else for (int candidates = material; candidates != 0; candidates &= candidates - 1) {
                int p = Integer.numberOfTrailingZeros(candidates);
                next.shares.set(i, p, next.shares.get(i, p) / retained);
            }
        }
        double[] belt = spread(uplift, 5 * resolution * resolution);
        for (int i = 0; i < cells; i++) next.height[i] += belt[i];
        return next;
    }

    private static double base(double land, double thickness, double age) {
        double continental = clamp(10 + 47 * (thickness / Math.max(1e-8, land) - 1), -32, 84);
        double oceanic = -18 - Math.min(14, age * 0.20);
        return mix(oceanic, continental, land);
    }

    private double[] spread(double[] field, int passes) {
        if (resolution >= 8) return spreadFine(field, passes);
        double[] tmp = new double[cells];
        for (int pass = 0; pass < passes; pass++) {
            for (int row = 0; row < cells; row += side) {
                int south = row + side == cells ? 0 : row + side;
                int north = row == 0 ? cells - side : row - side;
                int last = side - 1;
                tmp[row] = diffuse(field, row, row + 1, row + last, south, north);
                for (int x = 1; x < last; x++) {
                    int i = row + x;
                    tmp[i] = diffuse(field, i, i + 1, i - 1, south + x, north + x);
                }
                int end = row + last;
                tmp[end] = diffuse(field, end, row, end - 1, south + last, north + last);
            }
            double[] swap = tmp;
            tmp = field;
            field = swap;
        }
        return field;
    }

    private double[] spreadFine(double[] field, int passes) {
        // Each diffusion pass has variance 0.36 cells² per axis. Match that variance
        // with three separable box filters, each computed in one sweep per axis.
        double variance = 0.36 * passes;
        double width = Math.sqrt(4 * variance + 1);
        int lower = (int) Math.floor(width);
        if ((lower & 1) == 0) lower--;
        int upper = lower + 2;
        int useUpper = (int) Math.round((12 * variance - 3.0 * (lower * lower - 1))
                / (upper * upper - lower * lower));
        double[] tmp = new double[cells];
        double[] columnSums = new double[side];
        for (int pass = 0; pass < 3; pass++) {
            int radius = ((pass < useUpper ? upper : lower) - 1) / 2;
            blurRows(field, tmp, radius);
            blurColumns(tmp, field, radius, columnSums);
        }
        return field;
    }

    private void blurRows(double[] source, double[] target, int radius) {
        double weight = 1.0 / (2 * radius + 1);
        for (int line = 0; line < side; line++) {
            int base = line * side;
            double sum = 0;
            for (int offset = -radius; offset <= radius; offset++) {
                sum += source[base + Math.floorMod(offset, side)];
            }
            int leaving = base + side - radius, entering = base + radius + 1;
            for (int i = 0; i < side; i++) {
                target[base + i] = Math.max(0, sum) * weight;
                sum += source[entering] - source[leaving];
                if (++leaving == base + side) leaving = base;
                if (++entering == base + side) entering = base;
            }
        }
    }

    private void blurColumns(double[] source, double[] target, int radius, double[] sums) {
        double weight = 1.0 / (2 * radius + 1);
        Arrays.fill(sums, 0);
        for (int offset = -radius; offset <= radius; offset++) {
            int row = Math.floorMod(offset, side) * side;
            for (int x = 0; x < side; x++) sums[x] += source[row + x];
        }
        int leaving = (side - radius) * side, entering = (radius + 1) * side;
        for (int row = 0; row < cells; row += side) {
            for (int x = 0; x < side; x++) {
                target[row + x] = Math.max(0, sums[x]) * weight;
                sums[x] += source[entering + x] - source[leaving + x];
            }
            if ((leaving += side) == cells) leaving = 0;
            if ((entering += side) == cells) entering = 0;
        }
    }

    private static double diffuse(double[] field, int i, int east, int west, int south, int north) {
        double value = field[i];
        double sum = 0;
        sum += field[east] - value;
        sum += field[west] - value;
        sum += field[south] - value;
        sum += field[north] - value;
        return value + 0.18 * sum;
    }

    private void erode(double[] height) {
        Drainage water = drain(height);
        double[] change = new double[cells];
        for (int i : water.order) {
            int j = water.receiver[i];
            change[i] += 0.065 * laplacian(height, i);
            if (j < 0) continue;
            double slope = Math.max(0, height[i] - height[j]);
            double cut = Math.min(slope * 0.14, 0.015 * Math.sqrt(water.flow[i]) * slope);
            change[i] -= cut;
            change[j] += cut * (slope < 3 || water.sea[j] ? 0.55 : 0.25);
        }
        for (int i = 0; i < cells; i++) height[i] += change[i];
    }

    /** Largest contiguous below-sea region is the ocean; isolated depressions have outlets to test. */
    private boolean[] ocean(double[] height) {
        int[] region = new int[cells];
        Arrays.fill(region, -1);
        int largest = -2, biggest = 0;
        var queue = new ArrayDeque<Integer>();
        for (int start = 0; start < cells; start++) {
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
        boolean[] sea = new boolean[cells];
        for (int i = 0; i < cells; i++) sea[i] = region[i] == largest;
        return sea;
    }

    private Drainage drain(double[] height) {
        boolean[] sea = ocean(height);
        double[] rain = new double[cells];
        double[] spill = height.clone();
        var queue = new PriorityQueue<>(Comparator.<Integer>comparingDouble(i -> spill[i]).thenComparingInt(i -> i));
        int[] parent = new int[cells], order = new int[cells];
        Arrays.fill(parent, -2);
        for (int i = 0; i < cells; i++) {
            int upwind = neighbor(i, -5 * resolution, 0);
            rain[i] = sea[i] ? 0 : clamp(0.20 + 0.74 * field(i, 7, 481)
                    + clamp((height[i] - height[upwind]) * 0.009, -0.22, 0.28), 0.05, 1) / (resolution * resolution);
            if (sea[i]) {
                parent[i] = -1;
                spill[i] = 0;
                queue.add(i);
            }
        }
        if (queue.isEmpty()) throw new IllegalStateException("No ocean outlet");
        int rank = 0;
        int[] position = new int[cells];
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
        for (int k = cells - 1; k >= 0; k--) {
            int i = order[k];
            if (parent[i] >= 0) flow[parent[i]] += flow[i];
        }
        return new Drainage(parent, order, spill, flow, sea, rain);
    }

    private void export(Path dir, long seed) throws IOException {
        export(dir, seed, 1, false);
    }

    private void export(Path dir, long seed, int substeps, boolean traceRift) throws IOException {
        export(dir, seed, substeps, traceRift, false);
    }

    private void export(Path dir, long seed, int substeps, boolean traceRift, boolean timed) throws IOException {
        Files.createDirectories(dir);
        long phase = System.nanoTime();
        long tectonics = 0, erosion = 0, images = 0;
        double[] events = new double[2];
        Crust crust = initial;
        verifyPlates(initial.plate);
        writeFrame(dir, new Frame(0, crust));
        images += System.nanoTime() - phase;
        StringBuilder trace = new StringBuilder(
                "step,baseline_land_connected_to_ocean,low_land_not_connected_to_ocean,small_plate_fragments\n");
        boolean[] baselineSea = null;
        for (int step = 1; step <= STEPS; step++) {
            phase = System.nanoTime();
            for (int sub = 0; sub < substeps; sub++) crust = advance(crust, 1.0 / substeps, events);
            tectonics += System.nanoTime() - phase;
            phase = System.nanoTime();
            if (step % 2 == 0) erode(crust.height);
            erosion += System.nanoTime() - phase;
            if (step % 18 == 0 || (traceRift && step >= 18 && step <= 36)) {
                phase = System.nanoTime();
                writeFrame(dir, new Frame(step, crust));
                images += System.nanoTime() - phase;
                if (timed) System.out.printf("Step %d/%d: tectonics %.1f s, erosion %.1f s, maps %.1f s%n",
                        step, STEPS, seconds(tectonics), seconds(erosion), seconds(images));
            }
            if (traceRift && step >= 18 && step <= 36) {
                boolean[] sea = ocean(crust.height);
                if (baselineSea == null) baselineSea = sea;
                int flooded = 0, isolated = 0;
                // Lower-left landmass in seed 7, measured in the original preview coordinates.
                for (int z = 155; z < 225; z++) {
                    for (int x = 35; x < 110; x++) {
                        int i = z * side + x;
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
        int finalFragments = smallFragments(crust.plate);
        crust = null;
        phase = System.nanoTime();
        Drainage water = drain(height);
        verify(water);
        double[] evaporation = new double[cells];
        for (int i = 0; i < cells; i++) {
            evaporation[i] = (0.65 + 1.05 * field(i, 6, 294)) / (resolution * resolution);
        }
        var rivers = PreviewHydrology.build(side, resolution, height, water.sea, water.spill,
                water.order, water.receiver, water.rain, water.flow, evaporation);
        int seaRoutes = 0, basinRoutes = 0;
        int[] basinCells = new int[5];
        int[] previousChannels = new int[cells];
        int[] lengths = new int[cells];
        int longest = 0, junctions = 0;
        for (int i : water.order) {
            basinCells[rivers.surface()[i]]++;
            if (!rivers.channel()[i]) continue;
            if (rivers.inland()[i]) basinRoutes++; else seaRoutes++;
            int j = rivers.receiver()[i];
            if (rivers.channel()[j] && ++previousChannels[j] == 2) junctions++;
            // An inlet ends at the shore. An outlet is a separate visible reach.
            int end = i, length = 0;
            while (rivers.channel()[end] && lengths[end] == 0) {
                if (++length > cells) throw new IllegalStateException("Cyclic preview channel");
                int next = rivers.receiver()[end];
                if (rivers.surface()[next] != 0) {
                    end = -1;
                    break;
                }
                end = next;
            }
            lengths[i] = length + (end < 0 ? 0 : lengths[end]);
            longest = Math.max(longest, lengths[i]);
        }
        long hydrology = System.nanoTime() - phase;
        phase = System.nanoTime();
        write(dir, "plates", i -> plateColor(initial, i), true);
        write(dir, "initial", i -> relief(initial.height, i, initial.height[i] <= 0), false);
        write(dir, "evolved", i -> relief(height, i, water.sea[i]), false);
        writeDrainage(dir, height, water, rivers);
        images += System.nanoTime() - phase;
        if (timed) {
            long seaCells = 0;
            for (boolean sea : water.sea) if (sea) seaCells++;
            var timings = String.format(java.util.Locale.ROOT,
                    "tectonics_seconds=%.3f%nerosion_seconds=%.3f%nfinal_hydrology_seconds=%.3f%n"
                            + "map_export_seconds=%.3f%nopen_ocean_fraction=%.6f%n",
                    seconds(tectonics), seconds(erosion), seconds(hydrology), seconds(images), seaCells / (double) cells);
            Files.writeString(dir.resolve("timings.txt"), timings);
            System.out.print(timings);
        }
        int[] area = new int[PLATES];
        for (int p : initial.plate) area[p]++;
        long continental = java.util.stream.IntStream.range(0, cells).filter(i -> initial.land[i] > 0.5).count();
        int islands = countIslands(initial.land);
        double initialLow = Double.POSITIVE_INFINITY, initialHigh = Double.NEGATIVE_INFINITY;
        double highest = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < cells; i++) {
            if (initial.land[i] > 0.5) {
                initialLow = Math.min(initialLow, initial.height[i]);
                initialHigh = Math.max(initialHigh, initial.height[i]);
            }
            highest = Math.max(highest, height[i]);
        }
        Files.writeString(dir.resolve("summary.txt"), "seed=" + seed + "\nrequested land fraction=" + landFraction
                + "\ngrid cell size (blocks)=" + WorldTopology.DEFAULT_CELL / resolution
                + "\ninitial continental cells=" + continental + "\ninitial land masses=" + islands
                + "\ninitial land elevation range / final peak (above sea)="
                + "%.2f..%.2f / %.2f".formatted(initialLow, initialHigh, highest)
                + "\nsubsteps per iteration=" + substeps
                + "\ncontinental mass (initial / final)=" + startingMass + " / " + endingMass
                + "\ncollision / spreading area (cumulative grid cells)=" + events[0] + " / " + events[1]
                + "\nfinal small plate fragments (<16 cells)=" + finalFragments
                + "\ninitial plate area and velocities (grid x,z):\n"
                + java.util.stream.IntStream.range(0, PLATES)
                    .mapToObj(i -> i + ": " + area[i] + " cells, " + plates[i].vx + ", " + plates[i].vz + "\n")
                    .collect(java.util.stream.Collectors.joining())
                + "sea-bound channel cells=" + seaRoutes + "\ninterior-bound channel cells=" + basinRoutes
                + "\nchannel junctions=" + junctions
                + "\nlakes / flowing lake outlets=" + rivers.lakeCount() + " / " + rivers.outletCount()
                + "\ndry / seasonal / overflowing / closed basin cells=" + basinCells[1]
                + " / " + basinCells[2] + " / " + basinCells[3] + " / " + basinCells[4]
                + "\nlongest visible channel reach (grid segments)=" + longest + "\n");
    }

    private static double seconds(long nanoseconds) {
        return nanoseconds / 1e9;
    }

    private void verify(Drainage water) {
        int[] rank = new int[cells];
        for (int i = 0; i < cells; i++) rank[water.order[i]] = i;
        for (int i = 0; i < cells; i++) {
            int j = water.receiver[i];
            if ((j < 0 && !water.sea[i]) || (j >= 0 && (rank[j] >= rank[i] || water.spill[j] > water.spill[i]))) {
                throw new IllegalStateException("Invalid drain at cell " + i);
            }
        }
    }

    private void verifyPlates(int[] owner) {
        boolean[] visited = new boolean[cells];
        var pending = new ArrayDeque<Integer>();
        for (int p = 0; p < PLATES; p++) {
            int first = -1, area = 0;
            for (int i = 0; i < cells; i++) {
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

    private int smallFragments(int[] owner) {
        boolean[] visited = new boolean[cells];
        var queue = new ArrayDeque<Integer>();
        int count = 0;
        for (int first = 0; first < cells; first++) {
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

    private int countIslands(double[] land) {
        boolean[] visited = new boolean[cells];
        var queue = new ArrayDeque<Integer>();
        int count = 0;
        for (int first = 0; first < cells; first++) {
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

    private void writeDrainage(Path dir, double[] height, Drainage water,
                               PreviewHydrology.Result rivers) throws IOException {
        byte[] basin = rivers.surface();
        writeMap(dir, "drainage", 8, 2, (g, pixelsPerCell, region) -> {
            paintCells(g, pixelsPerCell, region, i -> {
                if (water.sea[i]) return relief(height, i, true);
                return switch (basin[i]) {
                    case 3 -> 0xff246bd6;
                    case 2 -> 0xff9377ce;
                    case 1 -> 0xffe1c585;
                    case 4 -> 0xff4d8fec;
                    default -> relief(height, i, false);
                };
            });
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            var line = new Line2D.Double();
            if (region.width == side && region.height == side) {
                for (int i : water.order) drawChannel(g, line, pixelsPerCell, i, water, rivers);
            } else {
                // The one-cell margin includes strokes that cross a tile or world edge.
                for (int z = region.z - 1; z <= region.z + region.height; z++) {
                    for (int x = region.x - 1; x <= region.x + region.width; x++) {
                        int i = Math.floorMod(z, side) * side + Math.floorMod(x, side);
                        drawChannel(g, line, pixelsPerCell, i, water, rivers);
                    }
                }
            }
        });
    }

    private void drawChannel(Graphics2D g, Line2D.Double line, int pixelsPerCell, int i,
                             Drainage water, PreviewHydrology.Result rivers) {
        if (!rivers.channel()[i]) return;
        int width = side * pixelsPerCell;
        int j = rivers.receiver()[i];
        int dx = localStep(i % side, j % side);
        int dz = localStep(i / side, j / side);
        // Stop at the water's edge instead of drawing across the receiving lake or sea.
        double length = water.sea[j] || rivers.surface()[j] != 0 ? 0.5 : 1.0;
        double start = rivers.outlet()[i] ? 0.5 : 0;
        double x = (i % side + 0.5) * pixelsPerCell;
        double z = (i / side + 0.5) * pixelsPerCell;
        double endX = x + dx * pixelsPerCell * length;
        double endZ = z + dz * pixelsPerCell * length;
        float stroke = (float) (pixelsPerCell
                * (0.14 + 0.25 * smooth(0, PreviewHydrology.MAIN_FLOW, rivers.discharge()[i])));
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(rivers.inland()[i] ? INLAND_CHANNEL : SEA_CHANNEL);
        int wrapX = endX <= 0 ? width : endX >= width ? -width : 0;
        int wrapZ = endZ <= 0 ? width : endZ >= width ? -width : 0;
        for (int tx = 0; tx <= (wrapX == 0 ? 0 : 1); tx++) {
            for (int tz = 0; tz <= (wrapZ == 0 ? 0 : 1); tz++) {
                line.setLine(x + dx * pixelsPerCell * start + tx * wrapX,
                        z + dz * pixelsPerCell * start + tz * wrapZ,
                        endX + tx * wrapX, endZ + tz * wrapZ);
                g.draw(line);
            }
        }
    }

    private int localStep(int from, int to) {
        return Math.floorMod(to - from + 1, side) - 1;
    }

    private record Region(int x, int z, int width, int height) {}
    private static final int TILE_CELLS = 128;

    @FunctionalInterface
    private interface MapPainter {
        void paint(Graphics2D g, int zoom, Region region);
    }

    private void writeMap(Path dir, String name, int zoom, int overviewZoom, MapPainter painter) throws IOException {
        Region world = new Region(0, 0, side, side);
        if (resolution < 8) {
            writeImage(dir.resolve(name + ".png"), zoom, world, painter);
            return;
        }
        writeImage(dir.resolve(name + ".png"), overviewZoom, world, painter);
        Path tiles = Files.createDirectories(dir.resolve("tiles").resolve(name));
        for (int z = 0; z < side; z += TILE_CELLS) {
            for (int x = 0; x < side; x += TILE_CELLS) {
                Region region = new Region(x, z, Math.min(TILE_CELLS, side - x), Math.min(TILE_CELLS, side - z));
                writeImage(tiles.resolve(x / TILE_CELLS + "-" + z / TILE_CELLS + ".png"), zoom, region, painter);
            }
        }
    }

    private static void writeImage(Path path, int zoom, Region region, MapPainter painter) throws IOException {
        var image = new BufferedImage(region.width * zoom, region.height * zoom, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        try {
            g.translate(-region.x * zoom, -region.z * zoom);
            painter.paint(g, zoom, region);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", path.toFile());
    }

    private void paintCells(Graphics2D g, int zoom, Region region, java.util.function.IntUnaryOperator color) {
        for (int z = region.z; z < region.z + region.height; z++) {
            for (int x = region.x; x < region.x + region.width; x++) {
                g.setColor(new Color(color.applyAsInt(z * side + x)));
                g.fillRect(x * zoom, z * zoom, zoom, zoom);
            }
        }
    }

    private void write(Path dir, String name, java.util.function.IntUnaryOperator color, boolean arrows)
            throws IOException {
        writeMap(dir, name, 4, 1, (g, zoom, region) -> {
            paintCells(g, zoom, region, color);
            if (arrows) {
                g.setColor(Color.WHITE);
                for (Plate plate : plates) {
                    double x = plate.origin % side * zoom, z = plate.origin / side * zoom;
                    int tx = (int) (x + plate.vx * 27), tz = (int) (z + plate.vz * 27);
                    g.drawLine((int) x, (int) z, tx, tz);
                    double len = Math.hypot(plate.vx, plate.vz);
                    for (int wing : new int[]{-1, 1}) {
                        g.drawLine(tx, tz, tx - (int) (7 * plate.vx / len + wing * 4 * plate.vz / len),
                                tz - (int) (7 * plate.vz / len - wing * 4 * plate.vx / len));
                    }
                }
            }
        });
    }

    private int relief(double[] height, int i, boolean sea) {
        double h = height[i];
        int light = (int) clamp((height[neighbor(i, -1, -1)] - height[neighbor(i, 1, 1)]) * 1.9, -34, 34);
        if (sea) return rgb(30 + light / 4, (int) (111 + h) + light / 3, (int) (160 + h) + light / 3);
        double rock = smooth(8, 56, h), snow = smooth(60, 110, h);
        return rgb((int) mix(mix(78, 158, rock), 234, snow) + light,
                (int) mix(mix(126, 139, rock), 236, snow) + light,
                (int) mix(mix(80, 123, rock), 239, snow) + light);
    }

    private double laplacian(double[] field, int i) {
        double sum = 0;
        for (int d = 0; d < DX.length; d++) sum += field[neighbor(i, DX[d], DZ[d])] - field[i];
        return sum;
    }

    private int neighbor(int i, int dx, int dz) {
        return Math.floorMod(i / side + dz, side) * side + Math.floorMod(i % side + dx, side);
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

    private static void writeDetailPage(Path dir, long[] seeds, double land) throws IOException {
        writeDetailPage(dir, seeds, land, false);
    }

    private static void writeDetailPage(Path dir, long[] seeds, double land, boolean fineOnly) throws IOException {
        var seedOptions = new StringBuilder();
        var fineDirectories = new StringBuilder();
        var tileDirectories = new StringBuilder();
        for (int i = 0; i < seeds.length; i++) {
            String path = detailSeedPath(i, seeds[i]);
            seedOptions.append("<option value=\"").append(path)
                    .append("\">").append(seeds[i]).append("</option>");
            if (Files.isRegularFile(dir.resolve(path + "64/summary.txt"))) {
                fineDirectories.append('"').append(path).append("\",");
            }
            if (Files.isRegularFile(dir.resolve(path + "32/summary.txt"))) {
                tileDirectories.append('"').append(path).append("\",");
            }
        }
        Files.writeString(dir.resolve("index.html"), """
                <!doctype html>
                <html lang="zh-CN">
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>地形采样对照</title>
                <style>
                  body { max-width: 90rem; margin: 2rem auto; padding: 0 1rem;
                         background: #17232a; color: #edf3f5; font: 1rem system-ui; }
                  main { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%%, 24rem), 1fr)); gap: 1rem; }
                  img { display: block; width: 100%%; height: auto; image-rendering: pixelated; }
                  img[src$="/drainage.png"] { image-rendering: auto; }
                  a { color: #a4daf3; }
                  select { font: inherit; }
                  dialog { width: min(90vw, 64rem); background: #17232a; color: #edf3f5; }
                  dialog img { width: auto; max-width: 100%%; margin: 0 auto; }
                </style>
                <h1>地形采样对照</h1>
                <p>初始陆地目标 %.0f%% · 世界边长 62,720 方块</p>
                <label>种子 <select id="seed">%s</select></label>
                <label>迭代轮次 <select id="step">
                  <option>00</option><option>18</option><option>36</option>
                  <option>54</option><option selected>72</option>
                </select></label>
                <label>图层 <select id="layer">
                  <option value="terrain">地形</option>
                  <option value="plates">板块</option>
                  <option value="thickness">陆壳厚度</option>
                  <option value="rift">张裂活动</option>
                  <option value="drainage">最终水文</option>
                </select></label>
                <p id="legend"></p>
                <main>
                  <section data-grid="256"><h2>256 方块 / 格</h2>
                    <a><img alt="256 方块网格的地形图"></a>
                    <p><a href="256/summary.txt" class="summary">统计</a> · 245 × 245 采样格</p>
                  </section>
                  <section data-grid="128"><h2>128 方块 / 格</h2>
                    <a><img alt="128 方块网格的地形图"></a>
                    <p><a href="128/summary.txt" class="summary">统计</a> · 490 × 490 采样格</p>
                  </section>
                  <section data-grid="64" hidden><h2>64 方块 / 格</h2>
                    <a><img alt="64 方块网格的地形图"></a>
                    <p><a class="summary">统计</a> · 980 × 980 采样格</p>
                  </section>
                  <section data-grid="32" hidden><h2>32 方块 / 格</h2>
                    <a><img alt="32 方块网格的地形图"></a>
                    <p><a class="summary">统计</a> · 1,960 × 1,960 采样格 · 点击地图查看局部原图</p>
                  </section>
                </main>
                <dialog id="detail-tile" aria-labelledby="tile-position">
                  <form method="dialog"><button>关闭</button></form>
                  <p id="tile-position"></p>
                  <button type="button" data-dx="-1" data-dz="0">向西</button>
                  <button type="button" data-dx="1" data-dz="0">向东</button>
                  <button type="button" data-dx="0" data-dz="-1">向北</button>
                  <button type="button" data-dx="0" data-dz="1">向南</button>
                  <img alt="所选区域的原始分辨率地图">
                </dialog>
                <p>各档地图覆盖同一范围。细网格重新推演后，局部海岸和河网可能变化。</p>
                <script>
                  const fineDirectories = new Set([%s]);
                  const tileDirectories = new Set([%s]);
                  const tileSize = %d;
                  const fineSide = %d;
                  const fineOnly = %s;
                  const layers = {
                    terrain: ['', '蓝色为海洋，绿色为低地，棕灰色为高地，白色为高山。'],
                    plates: ['-plates', '每种颜色代表一个板块编号。'],
                    thickness: ['-thickness', '越亮的地方陆壳越厚。'],
                    rift: ['-rift', '红色表示累计拉伸，蓝色表示新生洋壳。'],
                    drainage: ['', '水文显示第 72 轮。青色为入海河道，亮紫为内陆河道，深亮蓝为溢流湖，浅亮蓝为封闭湖，淡紫为季节湖，浅褐为干盆地。主河线条更粗。']
                  };
                  const seed = document.getElementById('seed');
                  const step = document.getElementById('step');
                  const layer = document.getElementById('layer');
                  function refresh() {
                    const [suffix, legend] = layers[layer.value];
                    step.disabled = layer.value === 'drainage';
                    const imageName = step.disabled ? 'drainage.png' : 'step-' + step.value + suffix + '.png';
                    document.getElementById('legend').textContent = legend;
                    for (const panel of document.querySelectorAll('[data-grid]')) {
                      panel.hidden = (fineOnly && ['256', '128'].includes(panel.dataset.grid))
                                  || (panel.dataset.grid === '64' && !fineDirectories.has(seed.value))
                                  || (panel.dataset.grid === '32' && !tileDirectories.has(seed.value));
                      if (panel.hidden) continue;
                      const directory = seed.value + panel.dataset.grid + '/';
                      const image = panel.querySelector('img');
                      panel.querySelector('a').href = directory + imageName;
                      panel.querySelector('.summary').href = directory + 'summary.txt';
                      image.src = directory + imageName;
                      image.alt = panel.dataset.grid + ' 方块网格的' + layer.selectedOptions[0].text + '图';
                    }
                  }
                  const fineLink = document.querySelector('[data-grid="32"] a');
                  const dialog = document.getElementById('detail-tile');
                  const tileCount = Math.ceil(fineSide / tileSize);
                  let tileX = 0, tileZ = 0, tilePath;
                  function showTile() {
                    dialog.querySelector('img').src = tilePath + tileX + '-' + tileZ + '.png';
                    document.getElementById('tile-position').textContent =
                      '地图瓦片 · 列 ' + (tileX + 1) + ' / ' + tileCount + ' · 行 ' + (tileZ + 1) + ' / ' + tileCount;
                  }
                  fineLink.addEventListener('click', event => {
                    if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
                    event.preventDefault();
                    const bounds = fineLink.querySelector('img').getBoundingClientRect();
                    const tileAt = ratio => Math.min(tileCount - 1, Math.max(0, Math.floor(ratio * fineSide / tileSize)));
                    tileX = event.detail && bounds.width ? tileAt((event.clientX - bounds.left) / bounds.width) : 0;
                    tileZ = event.detail && bounds.height ? tileAt((event.clientY - bounds.top) / bounds.height) : 0;
                    const path = fineLink.getAttribute('href');
                    const split = path.lastIndexOf('/') + 1;
                    tilePath = path.slice(0, split) + 'tiles/' + path.slice(split, -4) + '/';
                    showTile();
                    dialog.showModal();
                  });
                  for (const button of dialog.querySelectorAll('[data-dx]')) {
                    button.addEventListener('click', () => {
                      tileX = (tileX + Number(button.dataset.dx) + tileCount) %% tileCount;
                      tileZ = (tileZ + Number(button.dataset.dz) + tileCount) %% tileCount;
                      showTile();
                    });
                  }
                  for (const control of [seed, step, layer]) control.addEventListener('change', refresh);
                  refresh();
                </script>
                </html>
                """.formatted(land * 100, seedOptions, fineDirectories, tileDirectories, TILE_CELLS,
                        WorldTopology.DEFAULT_SIDE / 32, fineOnly));
    }

    private static String detailSeedPath(int index, long seed) {
        return index == 0 ? "" : "seeds/" + seed + "/";
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 4 && args[0].equals("--benchmark-32")) {
            Path dir = Path.of(args[1]);
            long seed = Long.parseLong(args[2]);
            double land = Double.parseDouble(args[3]);
            int scale = WorldTopology.DEFAULT_CELL / 32;
            long start = System.nanoTime();
            var prototype = new TectonicPrototype(seed, land, scale);
            long initialization = System.nanoTime() - start;
            System.out.printf("Initialized in %.3f s%n", seconds(initialization));
            prototype.export(dir.resolve("32"), seed, scale, false, true);
            writeDetailPage(dir, new long[]{seed}, land, true);
            var totals = String.format(java.util.Locale.ROOT, "initialization_seconds=%.3f%ntotal_seconds=%.3f%n",
                    seconds(initialization), seconds(System.nanoTime() - start));
            Files.writeString(dir.resolve("32/timings.txt"), totals, java.nio.file.StandardOpenOption.APPEND);
            System.out.print(totals);
            return;
        }
        if (args.length >= 4 && (args[0].equals("--detail") || args[0].equals("--detail-64")
                || args[0].equals("--detail-32"))) {
            Path dir = Path.of(args[1]);
            double land = Double.parseDouble(args[3]);
            int gridSize = switch (args[0]) {
                case "--detail-64" -> 64;
                case "--detail-32" -> 32;
                default -> 0;
            };
            boolean addFine = gridSize != 0;
            long[] seeds = java.util.stream.LongStream.concat(
                    java.util.stream.LongStream.of(Long.parseLong(args[2])),
                    Arrays.stream(args, 4, args.length).mapToLong(Long::parseLong)).distinct().toArray();
            if (addFine) {
                for (int i = 0; i < seeds.length; i++) {
                    long seed = seeds[i];
                    Path seedDir = dir.resolve(detailSeedPath(i, seed));
                    for (String grid : new String[]{"256", "128", "64", "32"}) {
                        Path summary = seedDir.resolve(grid).resolve("summary.txt");
                        if ((grid.equals("64") || grid.equals("32")) && !Files.exists(summary)) continue;
                        if (!Files.isRegularFile(summary) || !Files.readAllLines(summary).containsAll(Arrays.asList(
                                "seed=" + seed, "requested land fraction=" + land, "grid cell size (blocks)=" + grid))) {
                            throw new IllegalArgumentException("Existing comparison parameters mismatch: " + summary);
                        }
                    }
                }
            }
            for (int i = 0; i < seeds.length; i++) {
                long seed = seeds[i];
                Path seedDir = dir.resolve(detailSeedPath(i, seed));
                if (addFine) {
                    Path fine = seedDir.resolve(Integer.toString(gridSize));
                    if (!Files.isRegularFile(fine.resolve("summary.txt"))) {
                        int scale = WorldTopology.DEFAULT_CELL / gridSize;
                        System.out.printf("Generating seed %d at %d blocks per cell%n", seed, gridSize);
                        new TectonicPrototype(seed, land, scale).export(fine, seed, scale, false);
                    }
                    writeDetailPage(dir, seeds, land);
                } else {
                    new TectonicPrototype(seed, land).export(seedDir.resolve("256"), seed);
                    new TectonicPrototype(seed, land, 2).export(seedDir.resolve("128"), seed, 2, false);
                }
            }
            writeDetailPage(dir, seeds, land);
            return;
        }
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
                      img[src$="/drainage.png"]{image-rendering:auto}
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
                + "img[src$='/drainage.png']{image-rendering:auto}"
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
