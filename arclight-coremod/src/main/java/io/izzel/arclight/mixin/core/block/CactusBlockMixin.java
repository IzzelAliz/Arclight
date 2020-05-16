package io.izzel.arclight.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.CactusBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v1_14_R1.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CactusBlock.class)
public class CactusBlockMixin {

    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    public boolean arclight$blockGrow(World world, BlockPos pos, BlockState state) {
        return CraftEventFactory.handleBlockGrowEvent(world, pos, state);
    }
}
