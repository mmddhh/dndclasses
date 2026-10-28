package mdh.dndclasses.client;

import com.mojang.blaze3d.vertex.PoseStack;
import mdh.dndclasses.weapon.ThrownWeaponEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Renders a thrown weapon / launched ammunition entity.
 * <p>
 * Same pattern as vanilla {@code ArrowRenderer}: the inherited pose already sits at the entity,
 * so this only composes a rotation on top of it and never rebuilds the matrix. The shaft direction
 * comes from the projectile's own yaw/pitch (exactly the angles {@code AbstractArrow} maintains
 * from its velocity), so the impact orientation is the projectile's, not the camera's.
 * <p>
 * The sprite plane is built from the shaft and world up, so it never rotates when the player moves
 * or turns the camera.
 */
public class DndThrownWeaponRenderer extends EntityRenderer<ThrownWeaponEntity> {

    private final ItemRenderer itemRenderer;

    public DndThrownWeaponRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownWeaponEntity entity, float entityYaw, float partialTicks, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        ItemStack stack = entity.getDisplayStack();
        if (stack.isEmpty()) {
            return;
        }
        // Right after leaving the hand the projectile is basically inside the camera; hide it briefly.
        if (entity.tickCount < 2
                && entity.distanceToSqr(this.entityRenderDispatcher.camera.getEntity()) < 12.25D) {
            return;
        }

        // Same angles vanilla AbstractArrow stores from its velocity (yaw around Y, pitch up positive).
        float yaw = (float) Math.toRadians(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()));
        float pitch = (float) Math.toRadians(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot()));
        double horizontal = Math.cos(pitch);
        Vec3 shaft = new Vec3(Math.sin(yaw) * horizontal, Math.sin(pitch), Math.cos(yaw) * horizontal);

        float roll;
        if (entity.isStuckForRender()) {
            float shake = entity.shakeTime - partialTicks;
            roll = shake > 0.0F ? -Mth.sin(shake * 3.0F) * shake : 0.0F;
        } else {
            roll = (entity.tickCount + partialTicks) * ThrownWeaponEntity.SPIN_DEGREES_PER_TICK;
        }

        pose.pushPose();
        ProjectileRenderHelper.renderWeapon(pose, buffer, packedLight, this.itemRenderer, stack,
                shaft.toVector3f(), roll, entity.level(), entity.getId());
        pose.popPose();

        super.render(entity, entityYaw, partialTicks, pose, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownWeaponEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
