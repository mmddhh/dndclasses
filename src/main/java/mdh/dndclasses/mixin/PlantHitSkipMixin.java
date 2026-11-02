package mdh.dndclasses.mixin;

import mdh.dndclasses.weapon.DndWeapon;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While a D&amp;D weapon is held, plants (grass, flowers, crops, saplings, ...) no longer capture
 * the crosshair: the hit result becomes the entity behind them, or a miss. This stops left clicks
 * from breaking grass instead of attacking, which also consumed the swing and skipped the offhand
 * turn of the dual-wield alternation.
 */
@Mixin(Minecraft.class)
public abstract class PlantHitSkipMixin {

    @Shadow
    public Entity crosshairPickEntity;

    @Inject(method = "pick", at = @At("TAIL"))
    private void dndclasses$skipPlants(CallbackInfo ci) {
        Minecraft self = (Minecraft) (Object) this;
        if (self.player == null || self.level == null || self.hitResult == null) {
            return;
        }
        if (self.hitResult.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return;
        }
        ItemStack stack = self.player.getMainHandItem();
        if (!(stack.getItem() instanceof DndWeapon)) {
            return;
        }
        if (!(self.hitResult instanceof BlockHitResult blockHit)) {
            return;
        }
        BlockState state = self.level.getBlockState(blockHit.getBlockPos());
        boolean plant = state.getBlock() instanceof BushBlock
                || (state.canBeReplaced() && state.getFluidState().isEmpty());
        if (!plant) {
            return;
        }
        if (self.crosshairPickEntity != null && self.crosshairPickEntity != self.player) {
            self.hitResult = new EntityHitResult(self.crosshairPickEntity);
        } else {
            self.hitResult = BlockHitResult.miss(blockHit.getLocation(), blockHit.getDirection(),
                    blockHit.getBlockPos());
        }
    }
}
