package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.state.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FenceGateBlock.class)
public class FenceGateBlockMixin {

    // @formatter:off
    @Shadow @Final public static BooleanProperty POWERED;
    // @formatter:on

    @Redirect(method = "neighborChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isBlockPowered(Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean arclight$blockRedstone(World world, BlockPos pos, BlockState state) {
        boolean powered = world.isBlockPowered(pos);
        boolean oldPowered = state.get(POWERED);
        if (oldPowered != powered) {
            int newPower = powered ? 15 : 0;
            int oldPower = oldPowered ? 15 : 0;
            Block bukkitBlock = CraftBlock.at(world, pos);
            BlockRedstoneEvent eventRedstone = new BlockRedstoneEvent(bukkitBlock, oldPower, newPower);
            Bukkit.getPluginManager().callEvent(eventRedstone);
            return eventRedstone.getNewCurrent() > 0;
        }
        return powered;
    }
}
