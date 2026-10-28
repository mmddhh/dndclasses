package mdh.dndclasses.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * Drawing primitives for projectiles stuck into something.
 * <p>
 * Everything draws in the frame it is handed and never reads the camera, so the result is stable
 * while the player moves or turns.
 */
public final class ProjectileRenderHelper {

    /** Sprite items are 1 block wide; scale them down so a dagger reads like a dagger. */
    public static final float ITEM_SCALE = 0.8F;

    /** Vanilla {@code ArrowRenderer} model scale (1/16 block per model unit). */
    private static final float ARROW_SCALE = 0.05625F;

    private ProjectileRenderHelper() {
    }

    /**
     * Vanilla arrow rendering: yaw/pitch from the shaft direction, then the vanilla geometry
     * (45 degree roll, scale, -4 offset). Identical to what {@code ArrowRenderer} does, so a stuck
     * arrow looks like a vanilla arrow.
     */
    public static void renderArrow(PoseStack pose, MultiBufferSource buffer, int packedLight, Vector3f shaft) {
        Vector3f direction = new Vector3f(shaft);
        if (direction.lengthSquared() < 1.0E-8F) {
            return;
        }
        direction.normalize();

        float yaw = (float) (Math.atan2(direction.x, direction.z) * (180.0F / Math.PI));
        float pitch = (float) (Math.atan2(direction.y,
                Math.sqrt(direction.x * direction.x + direction.z * direction.z)) * (180.0F / Math.PI));

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(pitch));
        renderArrowGeometry(pose, buffer, packedLight);
        pose.popPose();
    }

    /** Draws an item sprite along {@code shaft} (thrown weapons stuck in a body). */
    public static void renderWeapon(PoseStack pose, MultiBufferSource buffer, int packedLight,
                                    ItemRenderer itemRenderer, ItemStack stack, Vector3f shaft,
                                    float rollDegrees, Level level, int seed) {
        Vector3f direction = new Vector3f(shaft);
        if (direction.lengthSquared() < 1.0E-8F) {
            return;
        }
        direction.normalize();

        // Sprite plane from the shaft and world up: fixed in the world, never camera-facing.
        Vector3f side = new Vector3f(direction).cross(new Vector3f(0.0F, 1.0F, 0.0F));
        if (side.lengthSquared() < 1.0E-6F) {
            side = new Vector3f(direction).cross(new Vector3f(1.0F, 0.0F, 0.0F));
        }
        if (side.lengthSquared() < 1.0E-6F) {
            return;
        }
        side.normalize();
        Vector3f xAxis = new Vector3f(direction).cross(side).normalize();

        pose.pushPose();
        pose.mulPose(new org.joml.Quaternionf().setFromNormalized(new org.joml.Matrix3f()
                .setColumn(0, xAxis)
                .setColumn(1, direction)
                .setColumn(2, side)));
        if (rollDegrees != 0.0F) {
            pose.mulPose(Axis.YP.rotationDegrees(rollDegrees));
        }
        pose.mulPose(Axis.ZP.rotationDegrees(45.0F));
        pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        itemRenderer.renderStatic(stack, net.minecraft.world.item.ItemDisplayContext.NONE, packedLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, pose, buffer, level, seed);
        pose.popPose();
    }

    /** Vanilla arrow mesh at the current pose, tip along local +X. */
    private static void renderArrowGeometry(PoseStack pose, MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(45.0F));
        pose.scale(ARROW_SCALE, ARROW_SCALE, ARROW_SCALE);
        pose.translate(-4.0F, 0.0F, 0.0F);

        com.mojang.blaze3d.vertex.VertexConsumer consumer = buffer.getBuffer(
                net.minecraft.client.renderer.RenderType.entityCutout(
                        net.minecraft.client.renderer.entity.TippableArrowRenderer.NORMAL_ARROW_LOCATION));

        // Shaft: two crossed quads.
        vertex(pose, consumer, -7, -2, -2, 0.0F, 0.15625F, -1, 0, 0, packedLight);
        vertex(pose, consumer, -7, -2, 2, 0.15625F, 0.15625F, -1, 0, 0, packedLight);
        vertex(pose, consumer, -7, 2, 2, 0.15625F, 0.3125F, -1, 0, 0, packedLight);
        vertex(pose, consumer, -7, 2, -2, 0.0F, 0.3125F, -1, 0, 0, packedLight);
        vertex(pose, consumer, -7, 2, -2, 0.0F, 0.15625F, 1, 0, 0, packedLight);
        vertex(pose, consumer, -7, 2, 2, 0.15625F, 0.15625F, 1, 0, 0, packedLight);
        vertex(pose, consumer, -7, -2, 2, 0.15625F, 0.3125F, 1, 0, 0, packedLight);
        vertex(pose, consumer, -7, -2, -2, 0.0F, 0.3125F, 1, 0, 0, packedLight);

        // Four fletching planes.
        for (int i = 0; i < 4; i++) {
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            vertex(pose, consumer, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, packedLight);
            vertex(pose, consumer, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, packedLight);
            vertex(pose, consumer, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, packedLight);
            vertex(pose, consumer, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, packedLight);
        }

        pose.popPose();
    }

    private static void vertex(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer consumer,
                               int x, int y, int z, float u, float v,
                               int normalX, int normalY, int normalZ, int packedLight) {
        org.joml.Matrix4f matrix = pose.last().pose();
        org.joml.Matrix3f normal = pose.last().normal();
        consumer.vertex(matrix, (float) x, (float) y, (float) z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, (float) normalX, (float) normalY, (float) normalZ)
                .endVertex();
    }
}
