package io.izzel.arclight.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.GrassBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GrassBlock.class)
public class GrassBlockMixin {

    @Redirect(method = "grow", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public boolean arclight$blockGrow(World world, BlockPos pos, BlockState newState, int flags) {
        return CraftEventFactory.handleBlockGrowEvent(world, pos, newState, flags);
    }
}
