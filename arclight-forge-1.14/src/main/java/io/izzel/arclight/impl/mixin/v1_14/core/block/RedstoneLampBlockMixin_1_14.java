package io.izzel.arclight.impl.mixin.v1_14.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(RedstoneLampBlock.class)
public class RedstoneLampBlockMixin_1_14 {

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private void arclight$redstoneChange(BlockState state, World worldIn, BlockPos pos, Random rand, CallbackInfo ci) {
        if (CraftEventFactory.callRedstoneChange(worldIn, pos, 15, 0).getNewCurrent() != 0) {
            ci.cancel();
        }
    }
}
