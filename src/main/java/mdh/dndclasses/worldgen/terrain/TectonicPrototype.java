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
    private static final double TURN = 2 * Math.PI;
    private static final int[] DX = {1, -1, 0, 0};
    private static final int[] DZ = {0, 0, 1, -1};

    private record Plate(double x, double z, double weight, double vx, double vz) {}
    private record Drainage(int[] receiver, int[] order, double[] spill, double[] flow) {}

    private final Plate[] plates = new Plate[PLATES];
    private final int[] owner = new int[CELLS];
    private final double[] initial = new double[CELLS];
    private final double[] forcing = new double[CELLS];
    private final double[] rain = new double[CELLS];

    public TectonicPrototype(long seed) {
        var rng = new SplittableRandom(seed);
        double p = rng.nextDouble(TURN);
        double q = rng.nextDouble(TURN);
        double r = rng.nextDouble(TURN);
        for (int i = 0; i < PLATES; i++) {
            double direction = rng.nextDouble(TURN);
            double speed = rng.nextDouble(0.35, 1.15);
            plates[i] = new Plate((i % 6 + rng.nextDouble(0.18, 0.82)) * SIDE / 6,
                    (i / 6 + rng.nextDouble(0.18, 0.82)) * SIDE / 3,
                    rng.nextDouble(0, 420), speed * Math.cos(direction), speed * Math.sin(direction));
        }
        for (int i = 0; i < CELLS; i++) {
            double x = angle(i % SIDE), z = angle(i / SIDE);
            double wx = i % SIDE + 7 * Math.sin(2 * z + p) + 4 * Math.sin(3 * x - z + q);
            double wz = i / SIDE + 7 * Math.sin(2 * x + q) + 4 * Math.cos(x + 3 * z + p);
            double best = Double.POSITIVE_INFINITY;
            for (int j = 0; j < PLATES; j++) {
                Plate plate = plates[j];
                double dx = delta(wx, plate.x), dz = delta(wz, plate.z);
                double score = dx * dx + dz * dz - plate.weight;
                if (score < best) {
                    best = score;
                    owner[i] = j;
                }
            }
            // Crust and plate ownership are independent. Interiors start near their base elevation.
            double crust = Math.sin(x + p) + 0.75 * Math.cos(z + q)
                    + 0.4 * Math.sin(2 * x - z + p) + 0.32 * Math.cos(x + 2 * z + r);
            initial[i] = -34 + 43 * smooth(-0.13, 0.32, crust)
                    + 1.5 * Math.sin(3 * x + 4 * z + q);
            rain[i] = clamp(0.54 + 0.24 * Math.sin(2 * z + q)
                    + 0.20 * Math.cos(x - 2 * z + p), 0.08, 1);
        }
        connectPlates();
        for (int i = 0; i < CELLS; i++) {
            Plate a = plates[owner[i]];
            for (int d = 0; d < DX.length; d++) {
                int j = neighbor(i, DX[d], DZ[d]);
                if (owner[i] == owner[j]) continue;
                Plate b = plates[owner[j]];
                double nx = delta(a.x, b.x), nz = delta(a.z, b.z);
                double length = Math.hypot(nx, nz);
                nx /= length;
                nz /= length;
                double vx = a.vx - b.vx, vz = a.vz - b.vz;
                double normal = vx * nx + vz * nz;
                double shear = Math.abs(vz * nx - vx * nz);
                boolean land = initial[i] > 0, otherLand = initial[j] > 0;
                double vertical = normal < 0 ? normal * 0.7
                        : normal * (land ? (otherLand ? 2.7 : 2.0) : (otherLand ? -1.7 : 0.8));
                forcing[i] += vertical + 0.11 * shear * Math.sin(5 * angle(i % SIDE) + 3 * angle(i / SIDE));
            }
        }
        // Spread boundary forces into belts without using elevation bands as mountain masks.
        double[] tmp = new double[CELLS];
        for (int pass = 0; pass < 12; pass++) {
            for (int i = 0; i < CELLS; i++) tmp[i] = forcing[i] + 0.23 * laplacian(forcing, i);
            System.arraycopy(tmp, 0, forcing, 0, CELLS);
        }
    }

    /** Preserve each plate's largest component and assign disconnected scraps by adjacency. */
    private void connectPlates() {
        int[] component = new int[CELLS];
        int[] largest = new int[PLATES], size = new int[PLATES];
        Arrays.fill(component, -1);
        var queue = new ArrayDeque<Integer>();
        for (int start = 0; start < CELLS; start++) {
            if (component[start] >= 0) continue;
            int p = owner[start], area = 0;
            component[start] = start;
            queue.add(start);
            while (!queue.isEmpty()) {
                int i = queue.remove();
                area++;
                for (int d = 0; d < DX.length; d++) {
                    int j = neighbor(i, DX[d], DZ[d]);
                    if (component[j] < 0 && owner[j] == p) {
                        component[j] = start;
                        queue.add(j);
                    }
                }
            }
            if (area > size[p]) {
                size[p] = area;
                largest[p] = start;
            }
        }
        for (int i = 0; i < CELLS; i++) {
            if (component[i] == largest[owner[i]]) queue.add(i);
            else owner[i] = -1;
        }
        while (!queue.isEmpty()) {
            int i = queue.remove();
            for (int d = 0; d < DX.length; d++) {
                int j = neighbor(i, DX[d], DZ[d]);
                if (owner[j] >= 0) continue;
                owner[j] = owner[i];
                queue.add(j);
            }
        }
    }

    private double[] evolve() {
        double[] height = initial.clone();
        double[] next = new double[CELLS];
        for (int step = 0; step < 32; step++) {
            Drainage water = drain(height);
            for (int i = 0; i < CELLS; i++) {
                next[i] = height[i] + 4.0 * forcing[i] + 0.10 * laplacian(height, i);
            }
            for (int i : water.order) {
                int j = water.receiver[i];
                if (j < 0 || height[i] <= 0) continue;
                double slope = Math.max(0, height[i] - height[j]);
                double cut = Math.min(slope * 0.17, 0.055 * Math.sqrt(water.flow[i]) * slope);
                next[i] -= cut;
                next[j] += cut * (slope < 3 || height[j] <= 0 ? 0.40 : 0.12);
            }
            double[] swap = height;
            height = next;
            next = swap;
        }
        return height;
    }

    private Drainage drain(double[] height) {
        double[] spill = height.clone();
        var queue = new PriorityQueue<>(Comparator.<Integer>comparingDouble(i -> spill[i]).thenComparingInt(i -> i));
        int[] parent = new int[CELLS], order = new int[CELLS];
        Arrays.fill(parent, -2);
        for (int i = 0; i < CELLS; i++) {
            if (height[i] <= 0) {
                parent[i] = -1;
                spill[i] = 0;
                queue.add(i);
            }
        }
        if (queue.isEmpty()) throw new IllegalStateException("No ocean outlet");
        int rank = 0;
        while (!queue.isEmpty()) {
            int i = queue.remove();
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
        double[] flow = rain.clone();
        for (int k = CELLS - 1; k >= 0; k--) {
            int i = order[k];
            if (parent[i] >= 0) flow[parent[i]] += flow[i];
        }
        return new Drainage(parent, order, spill, flow);
    }

    /** Classify each depression using its inflow, local humidity and evaporation. */
    private byte[] basins(double[] height, Drainage water) {
        byte[] kind = new byte[CELLS];
        var pending = new ArrayDeque<Integer>();
        var members = new ArrayDeque<Integer>();
        for (int i : water.order) {
            if (kind[i] != 0 || height[i] <= 0 || water.spill[i] - height[i] < 1) continue;
            double level = water.spill[i];
            double supply = 0, loss = 0, localRain = 0;
            kind[i] = -1;
            pending.add(i);
            while (!pending.isEmpty()) {
                int j = pending.remove();
                members.add(j);
                localRain += rain[j];
                loss += 0.7 + 0.75 * Math.abs(Math.sin(2 * angle(j / SIDE)));
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dz == 0) continue;
                        int k = neighbor(j, dx, dz);
                        if (water.spill[k] == level && height[k] > 0 && height[k] <= level - 1) {
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
        double[] height = evolve();
        Drainage water = drain(height);
        byte[] basin = basins(height, water);
        verify(water);
        int[] drop = new int[CELLS];
        int seaRoutes = 0, basinRoutes = 0, longest = 0;
        int[] basinCells = new int[4];
        int[] steps = new int[CELLS];
        for (int i : water.order) {
            basinCells[basin[i]]++;
            int j = water.receiver[i];
            drop[i] = basin[i] == 1 || basin[i] == 2 ? i : j < 0 ? -1 : drop[j];
            if (j >= 0 && basin[i] != 1 && basin[i] != 2) steps[i] = steps[j] + 1;
            if (water.flow[i] >= 24 && height[i] > 0 && basin[i] == 0) {
                if (drop[i] < 0) seaRoutes++; else basinRoutes++;
                longest = Math.max(longest, steps[i]);
            }
        }
        Files.createDirectories(dir);
        write(dir, "plates", i -> Color.HSBtoRGB(owner[i] * 0.618034f % 1, 0.55f, 0.87f), true);
        write(dir, "initial", i -> relief(initial[i]), false);
        write(dir, "evolved", i -> relief(height[i]), false);
        write(dir, "drainage", i -> {
            if (height[i] <= 0) return relief(height[i]);
            if (basin[i] == 3) return 0xff246bd6;
            if (basin[i] == 2) return 0xff9377ce;
            if (basin[i] == 1) return 0xffe1c585;
            if (water.flow[i] >= 24) return drop[i] < 0 ? 0xff35c7ee : 0xffad79ed;
            return relief(height[i]);
        }, false);
        Files.writeString(dir.resolve("summary.txt"), "seed=" + seed + "\nplate velocities (grid x,z):\n"
                + java.util.stream.IntStream.range(0, PLATES)
                    .mapToObj(i -> i + ": " + plates[i].vx + ", " + plates[i].vz + "\n")
                    .collect(java.util.stream.Collectors.joining())
                + "sea-bound channel cells=" + seaRoutes + "\ninterior-bound channel cells=" + basinRoutes
                + "\ndry / seasonal / overflowing basin cells=" + basinCells[1]
                + " / " + basinCells[2] + " / " + basinCells[3]
                + "\nlongest channel-to-outlet route (grid steps)=" + longest + "\n");
    }

    private void verify(Drainage water) {
        int[] rank = new int[CELLS];
        for (int i = 0; i < CELLS; i++) rank[water.order[i]] = i;
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
        for (int i = 0; i < CELLS; i++) {
            int j = water.receiver[i];
            if (j >= 0 && (rank[j] >= rank[i] || water.spill[j] > water.spill[i])) {
                throw new IllegalStateException("Invalid drain at cell " + i);
            }
        }
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
                    double x = plate.x * zoom, z = plate.z * zoom;
                    double a = Math.atan2(plate.vz, plate.vx);
                    int tx = (int) (x + plate.vx * 27), tz = (int) (z + plate.vz * 27);
                    g.drawLine((int) x, (int) z, tx, tz);
                    for (double wing : new double[]{a - 0.5, a + 0.5}) {
                        g.drawLine(tx, tz, tx - (int) (8 * Math.cos(wing)), tz - (int) (8 * Math.sin(wing)));
                    }
                }
            }
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", dir.resolve(name + ".png").toFile());
    }

    private static int relief(double height) {
        int h = (int) clamp(height * 1.4, 0, 130);
        if (height <= 0) return new Color(22, (int) clamp(89 + height, 25, 89),
                (int) clamp(149 + height, 55, 149)).getRGB();
        return new Color(65 + h, 109 + h, 61 + h).getRGB();
    }

    private static double laplacian(double[] field, int i) {
        double sum = 0;
        for (int d = 0; d < DX.length; d++) sum += field[neighbor(i, DX[d], DZ[d])] - field[i];
        return sum;
    }

    private static int neighbor(int i, int dx, int dz) {
        return Math.floorMod(i / SIDE + dz, SIDE) * SIDE + Math.floorMod(i % SIDE + dx, SIDE);
    }

    private static double angle(int coordinate) {
        return coordinate * TURN / SIDE;
    }

    private static double delta(double a, double b) {
        return Math.IEEEremainder(b - a, SIDE);
    }

    private static double smooth(double low, double high, double x) {
        double t = clamp((x - low) / (high - low), 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    public static void main(String[] args) throws IOException {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 12345L;
        new TectonicPrototype(seed).export(Path.of(args.length > 1 ? args[1] : "build/tectonic-preview"), seed);
    }
}
