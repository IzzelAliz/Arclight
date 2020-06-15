package io.izzel.arclight.common.mixin.v1_15.block;

import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.BooleanProperty;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(AbstractButtonBlock.class)
public class AbstractButtonBlockMixin_1_15 {

    // @formatter:off
    @Shadow @Final public static BooleanProperty POWERED;
    // @formatter:on

    @Inject(method = "onBlockActivated", cancellable = true, at = @At(value = "HEAD"))
    public void arclight$blockRedstone1(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit, CallbackInfoReturnable<Boolean> cir) {
        if (!state.get(POWERED)) {
            boolean powered = state.get(POWERED);
            Block block = CraftBlock.at(worldIn, pos);
            int old = (powered) ? 15 : 0;
            int current = (!powered) ? 15 : 0;

            BlockRedstoneEvent event = new BlockRedstoneEvent(block, old, current);
            Bukkit.getPluginManager().callEvent(event);

            if ((event.getNewCurrent() > 0) == (powered)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private void arclight$blockRedstone2(BlockState state, ServerWorld worldIn, BlockPos pos, Random rand, CallbackInfo ci) {
        Block block = CraftBlock.at(worldIn, pos);

        BlockRedstoneEvent event = new BlockRedstoneEvent(block, 15, 0);
        Bukkit.getPluginManager().callEvent(event);

        if (event.getNewCurrent() > 0) {
            ci.cancel();
        }
    }
}
