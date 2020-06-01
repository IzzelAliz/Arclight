package io.izzel.arclight.impl.mixin.v1_14.block;

import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(AbstractButtonBlock.class)
public class AbstractButtonBlockMixin_1_14 {

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private void arclight$blockRedstone2(BlockState state, World worldIn, BlockPos pos, Random random, CallbackInfo ci) {
        Block block = CraftBlock.at(worldIn, pos);

        BlockRedstoneEvent event = new BlockRedstoneEvent(block, 15, 0);
        Bukkit.getPluginManager().callEvent(event);

        if (event.getNewCurrent() > 0) {
            ci.cancel();
        }
    }
}
