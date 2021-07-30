package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(LecternBlock.class)
public class LecternBlockMixin {

    @Redirect(method = "popBook", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private BlockEntity arclight$noValidate(Level world, BlockPos pos) {
        return ((WorldBridge) world).bridge$getTileEntity(pos, false);
    }

    @Inject(method = "popBook", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Direction;getStepX()I"))
    private void arclight$returnIfEmpty(BlockState state, Level worldIn, BlockPos pos, CallbackInfo ci, BlockEntity tileEntity, LecternBlockEntity lecternTileEntity, Direction direction, ItemStack itemStack) {
        if (itemStack.isEmpty()) ci.cancel();
    }
}
