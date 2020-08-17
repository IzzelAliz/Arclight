package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.LeavesDecayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(LeavesBlock.class)
public class LeavesBlockMixin {

    @Inject(method = "randomTick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/block/LeavesBlock;spawnDrops(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"))
    public void arclight$leavesDecay(BlockState state, ServerWorld worldIn, BlockPos pos, Random random, CallbackInfo ci) {
        LeavesDecayEvent event = new LeavesDecayEvent(CraftBlock.at(worldIn, pos));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || worldIn.getBlockState(pos).getBlock() != (Object) this) {
            ci.cancel();
        }
    }
}
