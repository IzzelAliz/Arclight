package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockFromToEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(DragonEggBlock.class)
public class DragonEggBlockMixin {

    @Inject(method = "teleport", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/Level;isClientSide:Z"))
    public void arclight$blockFromTo(BlockState blockState, Level world, BlockPos blockPos, CallbackInfo ci,
                                     WorldBorder wb, int i, BlockPos pos) {
        org.bukkit.block.Block from = CraftBlock.at(world, blockPos);
        org.bukkit.block.Block to = CraftBlock.at(world, pos);
        BlockFromToEvent event = new BlockFromToEvent(from, to);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        } else {
            arclight$toBlock = new BlockPos(event.getToBlock().getX(), event.getToBlock().getY(), event.getToBlock().getZ());
        }
    }

    private transient BlockPos arclight$toBlock;

    @ModifyVariable(method = "teleport", index = 6, name = "blockpos", at = @At(value = "JUMP", opcode = Opcodes.IFEQ, ordinal = 1))
    public BlockPos arclight$setPos(BlockPos pos) {
        return arclight$toBlock;
    }

}
