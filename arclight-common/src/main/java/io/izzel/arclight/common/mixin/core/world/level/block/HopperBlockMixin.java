package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(HopperBlock.class)
public class HopperBlockMixin {

    private transient BlockEntity arclight$oldTicking;

    @Inject(method = "entityInside", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;entityInside(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/entity/HopperBlockEntity;)V"))
    private void arclight$captureHopper(BlockState blockState, Level level, BlockPos blockPos, Entity entity, CallbackInfo ci, BlockEntity blockEntity) {
        arclight$oldTicking = ArclightCaptures.getTickingBlockEntity();
        ArclightCaptures.captureTickingBlockEntity(blockEntity);
    }

    @Inject(method = "entityInside", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;entityInside(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/entity/HopperBlockEntity;)V"))
    private void arclight$resetHopper(BlockState blockState, Level level, BlockPos blockPos, Entity entity, CallbackInfo ci, BlockEntity blockEntity) {
        ArclightCaptures.captureTickingBlockEntity(arclight$oldTicking);
        arclight$oldTicking = null;
    }
}
