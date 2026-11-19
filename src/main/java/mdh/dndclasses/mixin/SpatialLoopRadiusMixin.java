package mdh.dndclasses.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Compatibility mixin for using SpatialLoop as an external prerequisite with a much larger world.
 * SpatialLoop 0.1.4 already allows large radii in {@code sanitizeRadius}; only the creation slider
 * still hard-codes 128. This is our integration code, not copied from SpatialLoop.
 */
@Mixin(targets = "cc.sighs.spatialloop.client.SpatialLoopRadiusSlider", remap = false)
public class SpatialLoopRadiusMixin {

    private static final double DND_MAX_RADIUS = 1_960.0D;
    private static final int DND_MAX_RADIUS_INT = 1_960;

    @ModifyConstant(method = {"<init>", "m_5697_", "spatialloop$applyExternalRadius"},
            constant = @Constant(doubleValue = 128.0D), remap = false)
    private static double dndclasses$raiseSliderScale(double original) {
        return DND_MAX_RADIUS;
    }

    @ModifyConstant(method = {"m_5697_", "spatialloop$attachInput"},
            constant = @Constant(intValue = 128), remap = false)
    private static int dndclasses$raiseSliderThreshold(int original) {
        return DND_MAX_RADIUS_INT;
    }
}
