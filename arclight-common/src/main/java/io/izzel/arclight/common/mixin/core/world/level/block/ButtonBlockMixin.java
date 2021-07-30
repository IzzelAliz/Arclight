package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(ButtonBlock.class)
public class ButtonBlockMixin {

    // @formatter:off
    @Shadow @Final public static BooleanProperty POWERED;
    // @formatter:on

    @Inject(method = "checkPressed", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getValue(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;"))
    public void arclight$entityInteract(BlockState state, Level worldIn, BlockPos pos, CallbackInfo ci,
                                        List<? extends Entity> list, boolean flag) {
        boolean flag1 = state.getValue(ButtonBlock.POWERED);
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
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public void arclight$blockRedstone3(BlockState state, Level worldIn, BlockPos pos, CallbackInfo ci,
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

    @Inject(method = "use", cancellable = true, at = @At(value = "HEAD"))
    public void arclight$blockRedstone1(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit, CallbackInfoReturnable<Boolean> cir) {
        if (!state.getValue(POWERED)) {
            boolean powered = state.getValue(POWERED);
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

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private void arclight$blockRedstone2(BlockState state, ServerLevel worldIn, BlockPos pos, Random rand, CallbackInfo ci) {
        Block block = CraftBlock.at(worldIn, pos);

        BlockRedstoneEvent event = new BlockRedstoneEvent(block, 15, 0);
        Bukkit.getPluginManager().callEvent(event);

        if (event.getNewCurrent() > 0) {
            ci.cancel();
        }
    }
}
