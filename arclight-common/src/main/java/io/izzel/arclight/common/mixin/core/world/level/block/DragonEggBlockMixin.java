package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockFromToEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DragonEggBlock.class)
public class DragonEggBlockMixin {

    @Decorate(method = "teleport", inject = true, at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/Level;isClientSide:Z"))
    private void arclight$blockFromTo(BlockState blockState, Level world, BlockPos blockPos,
                                     @Local(ordinal = -1) BlockPos pos) throws Throwable {
        org.bukkit.block.Block from = CraftBlock.at(world, blockPos);
        org.bukkit.block.Block to = CraftBlock.at(world, pos);
        BlockFromToEvent event = new BlockFromToEvent(from, to);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke();
            return;
        } else {
            pos = new BlockPos(event.getToBlock().getX(), event.getToBlock().getY(), event.getToBlock().getZ());
        }
        DecorationOps.blackhole().invoke(pos);
    }
}
