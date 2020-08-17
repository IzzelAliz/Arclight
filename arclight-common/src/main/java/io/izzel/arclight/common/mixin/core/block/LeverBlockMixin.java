package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeverBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.BooleanProperty;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeverBlock.class)
public class LeverBlockMixin {

    // @formatter:off
    @Shadow @Final public static BooleanProperty POWERED;
    // @formatter:on

    @Inject(method = "onBlockActivated", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/block/LeverBlock;setPowered(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    public void arclight$blockRedstone(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit, CallbackInfoReturnable<Boolean> cir) {
        boolean flag = state.get(POWERED);
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
