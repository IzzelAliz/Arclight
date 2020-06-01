package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.LecternTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(LecternBlock.class)
public class LecternBlockMixin {

    @Redirect(method = "dropBook", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getTileEntity(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/tileentity/TileEntity;"))
    private TileEntity arclight$noValidate(World world, BlockPos pos) {
        return ((WorldBridge) world).bridge$getTileEntity(pos, false);
    }

    @Inject(method = "dropBook", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Direction;getXOffset()I"))
    private void arclight$returnIfEmpty(BlockState state, World worldIn, BlockPos pos, CallbackInfo ci, TileEntity tileEntity, LecternTileEntity lecternTileEntity, Direction direction, ItemStack itemStack) {
        if (itemStack.isEmpty()) ci.cancel();
    }
}
