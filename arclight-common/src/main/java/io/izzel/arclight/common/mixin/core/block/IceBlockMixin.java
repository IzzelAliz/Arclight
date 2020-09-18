package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.IceBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IceBlock.class)
public class IceBlockMixin {

    @Inject(method = "turnIntoWater", cancellable = true, at = @At("HEAD"))
    public void arclight$blockFade(BlockState blockState, World world, BlockPos blockPos, CallbackInfo ci) {
        if (CraftEventFactory.callBlockFadeEvent(world, blockPos, world.getDimensionType().isUltrawarm()
            ? Blocks.AIR.getDefaultState() : Blocks.WATER.getDefaultState()).isCancelled()) {
            ci.cancel();
        }
    }
}
