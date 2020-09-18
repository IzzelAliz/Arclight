package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin {

    // @formatter:off
    @Shadow protected abstract int getStrongestSignal(World world, BlockPos pos);
    // @formatter:on

    @Redirect(method = "updatePower", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/RedstoneWireBlock;getStrongestSignal(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)I"))
    public int arclight$blockRedstone(RedstoneWireBlock redstoneWireBlock, World world, BlockPos pos, World world1, BlockPos pos1, BlockState state) {
        int i = this.getStrongestSignal(world, pos);
        int oldPower = state.get(RedstoneWireBlock.POWER);
        if (oldPower != i) {
            BlockRedstoneEvent event = new BlockRedstoneEvent(CraftBlock.at(world, pos), oldPower, i);
            Bukkit.getPluginManager().callEvent(event);
            i = event.getNewCurrent();
        }
        return i;
    }
}
