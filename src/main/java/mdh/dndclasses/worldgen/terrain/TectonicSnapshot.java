package mdh.dndclasses.worldgen.terrain;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Immutable world grid shared by the preview export and the block sampler. */
public final class TectonicSnapshot {
    public static final String ID = "seed7_land60_step15";
    public static final int SEA = 63;
    private static final int MAGIC = 0x444E4401;
    private static final short DRY = -65;
    private static final int[] DX = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] DZ = {-1, -1, -1, 0, 0, 1, 1, 1};

    private final WorldTopology topology;
    private final float[] height;
    private final short[] water;
    private final byte[] direction, width, surface;

    public record Sample(int ground, int water, boolean river, boolean ocean) {}

    private TectonicSnapshot(WorldTopology topology, float[] height, short[] water,
                             byte[] direction, byte[] width, byte[] surface) {
        this.topology = topology;
        this.height = height;
        this.water = water;
        this.direction = direction;
        this.width = width;
        this.surface = surface;
    }

    public static TectonicSnapshot load(String id) {
        if (!ID.equals(id)) throw new IllegalArgumentException("Unknown terrain snapshot: " + id);
        return Bundled.MAP;
    }

    private static final class Bundled {
        static final TectonicSnapshot MAP = loadBundled();
    }

    private static TectonicSnapshot loadBundled() {
        String resource = "/data/dndclasses/tectonic/" + ID + ".bin.gz";
        try (InputStream in = TectonicSnapshot.class.getResourceAsStream(resource)) {
            if (in == null) throw new FileNotFoundException(resource);
            return read(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load " + resource, e);
        }
    }

    public static TectonicSnapshot read(InputStream source) throws IOException {
        try (var in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(source)))) {
            if (in.readInt() != MAGIC || in.readInt() != WorldTopology.DEFAULT_SIDE || in.readInt() != 32) {
                throw new IOException("Unsupported tectonic snapshot");
            }
            var topology = new WorldTopology(WorldTopology.DEFAULT_SIDE, 32);
            int size = topology.gridSize() * topology.gridSize();
            float[] height = new float[size];
            short[] water = new short[size];
            byte[] direction = new byte[size], width = new byte[size], surface = new byte[size];
            for (int i = 0; i < size; i++) {
                height[i] = in.readFloat();
                water[i] = in.readShort();
                direction[i] = in.readByte();
                width[i] = in.readByte();
                surface[i] = in.readByte();
                if (!Float.isFinite(height[i]) || direction[i] < -1 || direction[i] >= DX.length
                        || width[i] < 0 || width[i] > 10 || surface[i] < 0 || surface[i] > 2) {
                    throw new IOException("Invalid snapshot cell " + i);
                }
            }
            if (in.read() != -1) throw new IOException("Trailing snapshot data");
            return new TectonicSnapshot(topology, height, water, direction, width, surface);
        }
    }

    static void write(Path file, TectonicSnapshot map) throws IOException {
        try (var out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(file))))) {
            out.writeInt(MAGIC);
            out.writeInt(map.topology.sideBlocks());
            out.writeInt(map.topology.cellSize());
            for (int i = 0; i < map.height.length; i++) {
                out.writeFloat(map.height[i]);
                out.writeShort(map.water[i]);
                out.writeByte(map.direction[i]);
                out.writeByte(map.width[i]);
                out.writeByte(map.surface[i]);
            }
        }
    }

    static TectonicSnapshot from(double[] terrain, boolean[] sea, double[] lakeLevel, PreviewHydrology.Result rivers) {
        var topology = new WorldTopology(WorldTopology.DEFAULT_SIDE, 32);
        int side = topology.gridSize(), size = terrain.length;
        float[] height = new float[size];
        short[] water = new short[size];
        Arrays.fill(water, DRY);
        byte[] direction = new byte[size], width = new byte[size], surface = new byte[size];
        Arrays.fill(direction, (byte) -1);
        int[] queue = new int[size];
        int tail = 0;
        for (int i = 0; i < size; i++) {
            height[i] = (float) terrain[i];
            if (sea[i]) {
                surface[i] = 1;
                water[i] = SEA;
            } else if (Double.isFinite(lakeLevel[i])) {
                surface[i] = 2;
                water[i] = level(lakeLevel[i]);
            }
            if (rivers.channel()[i]) {
                int j = rivers.receiver()[i];
                for (byte d = 0; d < DX.length; d++) {
                    if (neighbor(side, i, DX[d], DZ[d]) == j) direction[i] = d;
                }
                if (direction[i] < 0) throw new IllegalStateException("Nonlocal river edge: " + i);
                width[i] = (byte) Math.round(2 + 8 * Math.min(1, rivers.discharge()[i] / PreviewHydrology.MAIN_FLOW));
            }
            // A channel ending on an unmarked basin floor still needs a receiving column.
            if (surface[i] != 0 || !rivers.channel()[i]) queue[tail++] = i;
        }
        for (int head = 0; head < tail; head++) {
            int downstream = queue[head];
            int bottom = water[downstream] == DRY ? level(terrain[downstream]) : water[downstream];
            for (int d = 0; d < DX.length; d++) {
                int i = neighbor(side, downstream, -DX[d], -DZ[d]);
                if (direction[i] != d || water[i] != DRY) continue;
                water[i] = (short) Math.max(bottom, Math.min(bottom + 12, level(terrain[i])));
                queue[tail++] = i;
            }
        }
        for (int i = 0; i < size; i++) {
            if (width[i] > 0 && water[i] == DRY) throw new IllegalStateException("Unresolved river level: " + i);
        }
        return new TectonicSnapshot(topology, height, water, direction, width, surface);
    }

    private static short level(double relative) {
        return (short) Math.max(-60, Math.min(440, SEA + Math.round(relative)));
    }

    public Sample sample(int x, int z) {
        int cell = topology.cellSize(), side = topology.gridSize();
        int wx = topology.wrap(x), wz = topology.wrap(z);
        int nearest = wz / cell * side + wx / cell;
        int ax = topology.wrap(wx - cell / 2), az = topology.wrap(wz - cell / 2);
        int a = az / cell * side + ax / cell;
        double tx = (ax % cell) / (double) cell, tz = (az % cell) / (double) cell;
        int ground = level(lerp(lerp(height[a], height[neighbor(side, a, 1, 0)], tx),
                lerp(height[neighbor(side, a, 0, 1)], height[neighbor(side, a, 1, 1)], tx), tz));
        int wet = surface[nearest] == 0 ? DRY : water[nearest];
        if (wet != DRY) ground = Math.min(ground, wet - 1);
        else {
            if (surface[neighbor(side, nearest, 1, 0)] == 1 || surface[neighbor(side, nearest, -1, 0)] == 1
                    || surface[neighbor(side, nearest, 0, 1)] == 1 || surface[neighbor(side, nearest, 0, -1)] == 1) {
                ground = Math.max(ground, SEA);
            }
        }
        boolean river = false;
        int riverWater = DRY;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int i = neighbor(side, nearest, dx, dz), d = direction[i];
                if (d < 0) continue;
                int rx = wx % cell - cell / 2 - dx * cell, rz = wz % cell - cell / 2 - dz * cell;
                int sx = DX[d] * cell, sz = DZ[d] * cell;
                double t = Math.max(0, Math.min(1, (rx * sx + rz * sz) / (double) (sx * sx + sz * sz)));
                double ex = rx - t * sx, ez = rz - t * sz;
                if (ex * ex + ez * ez > width[i] * width[i]) continue;
                int j = neighbor(side, i, DX[d], DZ[d]);
                int end = water[j] == DRY ? level(height[j]) : water[j];
                riverWater = Math.max(riverWater, (int) Math.round(lerp(water[i], end, t)));
                river = true;
            }
        }
        if (river && wet == DRY) {
            wet = riverWater;
            ground = Math.min(ground, wet - 2);
        }
        return new Sample(ground, wet, river && surface[nearest] == 0, surface[nearest] == 1);
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    private static int neighbor(int side, int i, int dx, int dz) {
        return Math.floorMod(i / side + dz, side) * side + Math.floorMod(i % side + dx, side);
    }
}
