package io.izzel.arclight.mixin.core.block;

import io.izzel.arclight.bridge.entity.EntityBridge;
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_14_R1.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Random;

@Mixin(AbstractButtonBlock.class)
public class AbstractButtonBlockMixin {

    @Inject(method = "onBlockActivated", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void arclight$blockRedstone1(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit, CallbackInfoReturnable<Boolean> cir) {
        boolean powered = state.get(AbstractButtonBlock.POWERED);
        Block block = CraftBlock.at(worldIn, pos);
        int old = (powered) ? 15 : 0;
        int current = (!powered) ? 15 : 0;

        BlockRedstoneEvent event = new BlockRedstoneEvent(block, old, current);
        Bukkit.getPluginManager().callEvent(event);

        if ((event.getNewCurrent() > 0) == (powered)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void arclight$blockRedstone2(BlockState state, World worldIn, BlockPos pos, Random random, CallbackInfo ci) {
        Block block = CraftBlock.at(worldIn, pos);

        BlockRedstoneEvent event = new BlockRedstoneEvent(block, 15, 0);
        Bukkit.getPluginManager().callEvent(event);

        if (event.getNewCurrent() > 0) {
            ci.cancel();
        }
    }

    @Inject(method = "checkPressed", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;get(Lnet/minecraft/state/IProperty;)Ljava/lang/Comparable;"))
    public void arclight$entityInteract(BlockState state, World worldIn, BlockPos pos, CallbackInfo ci,
                                        List<? extends Entity> list, boolean flag) {
        boolean flag1 = state.get(AbstractButtonBlock.POWERED);
        if (flag1 != flag && flag) {
            Block block = CraftBlock.at(worldIn, pos);
            boolean allowed = false;

            for (Object object : list) {
                if (object != null) {
                    EntityInteractEvent event = new EntityInteractEvent(((EntityBridge) object).bridge$getBukkitEntity(), block);
                    Bukkit.getPluginManager().callEvent(event);

                    if (!event.isCancelled()) {
                        allowed = true;
                        break;
                    }
                }
            }

            if (!allowed) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "checkPressed", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void arclight$blockRedstone3(BlockState state, World worldIn, BlockPos pos, CallbackInfo ci,
                                        List<? extends Entity> list, boolean flag, boolean flag1) {
        Block block = CraftBlock.at(worldIn, pos);
        int old = (flag1) ? 15 : 0;
        int current = (!flag1) ? 15 : 0;

        BlockRedstoneEvent event = new BlockRedstoneEvent(block, old, current);
        Bukkit.getPluginManager().callEvent(event);

        if ((flag && event.getNewCurrent() <= 0) || (!flag && event.getNewCurrent() > 0)) {
            ci.cancel();
        }
    }
}
