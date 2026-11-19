package mdh.dndclasses.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Minimal compatibility experiment for using SpatialLoop as an external prerequisite with a world
 * larger than its current 128-chunk slider limit. This is our integration code, not copied from
 * SpatialLoop.
 */
@Mixin(targets = "cc.sighs.spatialloop.geometry.LoopGeometry", remap = false)
public class SpatialLoopRadiusMixin {

    private static final int DND_MAX_RADIUS = 1_960;

    @ModifyConstant(method = "sanitizeRadius", constant = @Constant(intValue = 128),
            remap = false)
    private static int dndclasses$raiseMaxRadius(int original) {
        return DND_MAX_RADIUS;
    }
}
