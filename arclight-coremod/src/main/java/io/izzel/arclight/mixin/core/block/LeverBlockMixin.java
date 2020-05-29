package io.izzel.arclight.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeverBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(LeverBlock.class)
public class LeverBlockMixin {

    @Inject(method = "onBlockActivated", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD
        , at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void arclight$blockRedstone(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit, CallbackInfoReturnable<Boolean> cir,
                                       boolean flag) {
        Block block = CraftBlock.at(worldIn, pos);
        int old = (flag) ? 15 : 0;
        int current = (!flag) ? 15 : 0;

        BlockRedstoneEvent eventRedstone = new BlockRedstoneEvent(block, old, current);
        Bukkit.getPluginManager().callEvent(eventRedstone);

        if ((eventRedstone.getNewCurrent() > 0) == flag) {
            cir.setReturnValue(true);
        }
    }
}
