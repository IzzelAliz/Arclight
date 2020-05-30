package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PoweredRailBlock.class)
public class PoweredRailBlockMixin {

    @Inject(method = "updateState", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void arclight$blockRedstone(BlockState state, World worldIn, BlockPos pos, Block blockIn, CallbackInfo ci, boolean flag) {
        int power = flag ? 15 : 0;
        int newPower = CraftEventFactory.callRedstoneChange(worldIn, pos, power, 15 - power).getNewCurrent();
        if (newPower == power) {
            ci.cancel();
        }
    }
}
