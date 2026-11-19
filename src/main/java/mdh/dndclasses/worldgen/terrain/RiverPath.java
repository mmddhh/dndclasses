package mdh.dndclasses.worldgen.terrain;

import java.util.List;

/** Ordered river polyline in coarse grid coordinates (source to sea). */
public record RiverPath(List<Node> points, int width) {

    public record Node(int x, int z) {
    }
}
