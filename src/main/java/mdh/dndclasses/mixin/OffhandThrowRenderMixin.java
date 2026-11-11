package mdh.dndclasses.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import mdh.dndclasses.weapon.DndCooldowns;
import mdh.dndclasses.weapon.DndWeapon;
import mdh.dndclasses.weapon.WeaponProperty;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Last-resort visual guard: while an offhand throw is in progress, force the first-person renderer
 * to treat the swing as an offhand swing and freeze the main hand's equip/use offset. Some animation
 * paths (item use, equip progress, an already-running main swing) are not reachable through
 * {@code swing()} or {@code itemUsed()}, so they are neutralised at render time instead.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class OffhandThrowRenderMixin {

    @Shadow
    private float mainHandHeight;

    @Shadow
    private float oMainHandHeight;

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void dndclasses$freezeMainHand(float partialTicks, PoseStack poseStack,
                                           MultiBufferSource.BufferSource buffer,
                                           LocalPlayer player, int combinedLight, CallbackInfo ci) {
        ItemStack offhand = player.getOffhandItem();
        if (!(offhand.getItem() instanceof DndWeapon weapon)
                || !weapon.spec().isThrown() || !weapon.spec().has(WeaponProperty.LIGHT)) {
            return;
        }
        if (!DndCooldowns.isThrowWindow(player, 2)) {
            return;
        }
        if (player.swinging && player.swingingArm == InteractionHand.MAIN_HAND) {
            // Kill any main-hand swing that was already running when the throw started.
            player.swinging = false;
            player.swingTime = 0;
        }
        player.swingingArm = InteractionHand.OFF_HAND;
        this.mainHandHeight = 1.0F;
        this.oMainHandHeight = 1.0F;
    }
}
