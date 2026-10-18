package mdh.dndclasses.conditions;

import java.util.UUID;

/** Live instance of an active condition. */
public final class ActiveCondition {

    public int durationTicks;
    public int dc;
    public int saveTicks;
    public UUID source;

    public ActiveCondition(int durationTicks, int dc, int saveTicks, UUID source) {
        this.durationTicks = durationTicks;
        this.dc = dc;
        this.saveTicks = saveTicks;
        this.source = source;
    }
}
