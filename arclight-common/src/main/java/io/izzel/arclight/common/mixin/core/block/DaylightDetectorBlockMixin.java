package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.DaylightDetectorBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DaylightDetectorBlock.class)
public class DaylightDetectorBlockMixin {

    @ModifyVariable(method = "updatePower", index = 3, name = "i",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private static int arclight$blockRedstone(int i, BlockState blockState, World world, BlockPos blockPos) {
        return CraftEventFactory.callRedstoneChange(world, blockPos, blockState.get(DaylightDetectorBlock.POWER), i).getNewCurrent();
    }
}
