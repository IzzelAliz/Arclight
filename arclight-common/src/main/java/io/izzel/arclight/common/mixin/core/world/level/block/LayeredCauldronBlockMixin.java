package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mod.server.block.CauldronHooks;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayeredCauldronBlock.class)
public class LayeredCauldronBlockMixin {

    @Redirect(method = "entityInside", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;clearFire()V"))
    private void arclight$extinguish1(Entity entity) {
    }

    @Inject(method = "entityInside", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/LayeredCauldronBlock;handleEntityOnFireInside(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void arclight$extinguish2(BlockState p_153534_, Level p_153535_, BlockPos p_153536_, Entity entity, CallbackInfo ci) {
        CauldronHooks.setChangeReason(entity, CauldronLevelChangeEvent.ChangeReason.EXTINGUISH);
    }

    @Inject(method = "entityInside", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/block/LayeredCauldronBlock;handleEntityOnFireInside(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void arclight$extinguish3(BlockState p_153534_, Level p_153535_, BlockPos p_153536_, Entity p_153537_, CallbackInfo ci) {
        if (!CauldronHooks.getResult()) {
            ci.cancel();
        }
        CauldronHooks.reset();
    }

    @Redirect(method = "lowerFillLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private static boolean arclight$lowerFill(Level level, BlockPos pos, BlockState state, BlockState old) {
        return CauldronHooks.changeLevel(old, level, pos, state, CauldronHooks.getEntity(), CauldronHooks.getReason());
    }

    @Redirect(method = "handlePrecipitation", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean arclight$precipitation(Level level, BlockPos pos, BlockState state, BlockState old) {
        return CauldronHooks.changeLevel(old, level, pos, state, null, CauldronLevelChangeEvent.ChangeReason.NATURAL_FILL);
    }

    @Eject(method = "receiveStalactiteDrip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean arclight$drip(Level level, BlockPos pos, BlockState state, CallbackInfo ci, BlockState old) {
        if (CauldronHooks.changeLevel(old, level, pos, state, null, CauldronLevelChangeEvent.ChangeReason.NATURAL_FILL)) {
            return true;
        } else {
            ci.cancel();
            return false;
        }
    }
}
