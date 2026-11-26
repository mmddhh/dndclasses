package mdh.dndclasses.worldgen.terrain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A deterministic network of bent ridge skeletons. Each segment is a triangular prism in
 * cross-section: the crest is a line, the two flanks ease into the surrounding terrain, and both ends taper
 * down to the surrounding foot instead of ending in a vertical triangular face.
 */
public final class RidgeNetwork {

    public record Sample(double height, double weight) {
        public static final Sample NONE = new Sample(0.0D, 0.0D);
    }

    private record Node(int x, int z, double crest) {
    }

    private record Ridge(List<Node> nodes, double width) {
    }

    private final WorldTopology topology;
    private final List<Ridge> ridges;

    public RidgeNetwork(WorldTopology topology, long seed) {
        this.topology = topology;
        this.ridges = new ArrayList<>();
        Random random = new Random(seed ^ 0x6A09E667F3BCC909L);
        int count = 18;
        for (int i = 0; i < count; i++) {
            int x = random.nextInt(topology.sideBlocks());
            int z = random.nextInt(topology.sideBlocks());
            int points = 5 + random.nextInt(6);
            List<Node> nodes = new ArrayList<>();
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double width = 520.0D + random.nextDouble() * 680.0D;
            double baseCrest = 180.0D + random.nextDouble() * 130.0D;
            for (int p = 0; p < points; p++) {
                double crest = baseCrest + (random.nextDouble() - 0.5D) * 90.0D;
                nodes.add(new Node(x, z, crest));
                angle += (random.nextDouble() - 0.5D) * 0.9D;
                int step = 1_700 + random.nextInt(2_200);
                x = topology.wrap(x + (int) Math.round(Math.cos(angle) * step));
                z = topology.wrap(z + (int) Math.round(Math.sin(angle) * step));
            }
            ridges.add(new Ridge(nodes, width));
        }
    }

    public Sample sample(int x, int z) {
        double bestWeight = 0.0D;
        double bestHeight = 0.0D;
        for (Ridge ridge : ridges) {
            List<Node> nodes = ridge.nodes();
            for (int i = 1; i < nodes.size(); i++) {
                Node a = nodes.get(i - 1);
                Node b = nodes.get(i);
                double ax = a.x();
                double az = a.z();
                double bx = b.x();
                double bz = b.z();
                double dx = delta(bx - ax);
                double dz = delta(bz - az);
                double lengthSq = dx * dx + dz * dz;
                if (lengthSq < 1.0D) continue;
                double px = delta(x - ax);
                double pz = delta(z - az);
                double t = (px * dx + pz * dz) / lengthSq;
                t = Math.max(0.0D, Math.min(1.0D, t));
                double nearestX = ax + dx * t;
                double nearestZ = az + dz * t;
                double lateral = Math.sqrt(squaredDelta(x, nearestX) + squaredDelta(z, nearestZ));
                double halfWidth = ridge.width() * 0.5D;
                if (lateral >= halfWidth) continue;
                double foot = Math.min(1.0D, Math.min(t, 1.0D - t) / 0.14D);
                foot = foot * foot * (3.0D - 2.0D * foot);
                double u = lateral / halfWidth;
                double profile = 1.0D - u * u * (3.0D - 2.0D * u);
                double weight = foot * profile;
                double crest = a.crest() + (b.crest() - a.crest()) * t;
                if (weight > bestWeight) {
                    bestWeight = weight;
                    bestHeight = crest * weight;
                }
            }
        }
        return bestWeight <= 0.0D ? Sample.NONE : new Sample(bestHeight, bestWeight);
    }

    private double squaredDelta(int a, double b) {
        double d = delta(a - b);
        return d * d;
    }

    private double delta(double value) {
        return topology.center((int) Math.round(value));
    }
}
