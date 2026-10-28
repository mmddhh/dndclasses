package mdh.dndclasses.client;

import com.mojang.blaze3d.vertex.PoseStack;
import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.gear.StuckProjectile;
import mdh.dndclasses.gear.StuckProjectiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Draws the projectiles stored in {@link mdh.dndclasses.gear.IStuckProjectiles} on living entities.
 * <p>
 * Same idea as vanilla's stuck-arrow handling: each projectile is jumped to a pseudo-random spot on
 * the body (seeded from the entity id, so the layout is stable), with a random angle, and drawn
 * with the vanilla arrow mesh (or its own item, so a silvered dagger still looks like a silvered
 * dagger).
 * <p>
 * {@code RenderLivingEvent.Post} fires after the model transform has been popped, so the pose
 * origin is the entity position with world axes; only a relative {@code translate} is needed and
 * the camera rotation that sits outermost in the stack is untouched. Orientation comes from the
 * random direction only, never from the camera.
 */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StuckProjectileRenderEvents {

    private StuckProjectileRenderEvents() {
    }

    @SubscribeEvent
    public static void onRenderPost(RenderLivingEvent.Post<LivingEntity, EntityModel<LivingEntity>> event) {
        LivingEntity entity = event.getEntity();
        if (entity.isInvisible()) {
            return;
        }
        List<StuckProjectile> projectiles = StuckProjectiles.listOf(entity);
        if (projectiles.isEmpty()) {
            return;
        }

        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int packedLight = event.getPackedLight();
        float partialTicks = event.getPartialTick();
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        float width = entity.getBbWidth();
        float height = entity.getBbHeight();
        float bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        RandomSource random = RandomSource.create((long) entity.getId());

        for (int i = 0; i < projectiles.size(); i++) {
            StuckProjectile projectile = projectiles.get(i);
            if (projectile.stack().isEmpty()) {
                continue;
            }

            // Place the projectile on the body surface and aim it at the body axis (in body space),
            // then rotate by the current body yaw: the shots turn with the entity, always point
            // into the body and sit flush with the surface instead of floating beside it.
            float angle = random.nextFloat() * (float) (Math.PI * 2.0);
            float radius = width * 0.45F;
            float x = Mth.cos(angle) * radius;
            float z = Mth.sin(angle) * radius;
            float y = height * (0.3F + random.nextFloat() * 0.5F);

            float tilt = (random.nextFloat() - 0.5F) * 0.5F;
            Vec3 shaft = StuckProjectile.bodyToWorld(
                    new Vec3(-Mth.cos(angle), tilt, -Mth.sin(angle)).normalize(), bodyYaw);
            Vec3 offset = StuckProjectile.bodyToWorld(new Vec3(x, y, z), bodyYaw);

            pose.pushPose();
            pose.translate(offset.x, offset.y, offset.z);
            if (projectile.kind() == StuckProjectile.Kind.ARROW) {
                ProjectileRenderHelper.renderArrow(pose, buffer, packedLight, shaft.toVector3f());
            } else {
                ProjectileRenderHelper.renderWeapon(pose, buffer, packedLight, itemRenderer,
                        projectile.stack(), shaft.toVector3f(), 0.0F, entity.level(),
                        entity.getId() * 31 + projectile.id());
            }
            pose.popPose();
        }
    }
}
