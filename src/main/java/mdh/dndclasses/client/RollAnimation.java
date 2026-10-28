package mdh.dndclasses.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mdh.dndclasses.Dndclasses;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/** Third-person somersault + a subtle first-person tilt while rolling. */
@Mod.EventBusSubscriber(modid = Dndclasses.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RollAnimation {

    private static final Set<Integer> PUSHED = new HashSet<>();

    private RollAnimation() {
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        RollAnimationState.Animation animation = RollAnimationState.get(player.getId());
        if (animation == null) {
            return;
        }
        float progress = RollAnimationState.progress(animation, event.getPartialTick());
        if (progress >= 1.0F) {
            return;
        }

        // The yaw already follows the movement direction, so backward rolls reverse the
        // rotation axis on their own; do not flip the angle a second time here.
        float angle = 360.0F * progress;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(-animation.yaw()));
        pose.mulPose(Axis.XP.rotationDegrees(angle));
        pose.mulPose(Axis.YP.rotationDegrees(animation.yaw()));
        pose.translate(0.0D, -player.getBbHeight() * 0.5D, 0.0D);
        PUSHED.add(player.getId());
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (PUSHED.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        RollAnimationState.Animation animation = RollAnimationState.get(minecraft.player.getId());
        if (animation == null) {
            return;
        }
        float progress = RollAnimationState.progress(animation, (float) event.getPartialTick());
        if (progress >= 1.0F) {
            return;
        }
        float tilt = (animation.back() ? -1.0F : 1.0F) * 18.0F * Mth.sin(progress * (float) Math.PI);
        event.setRoll(event.getRoll() + tilt);
    }
}
