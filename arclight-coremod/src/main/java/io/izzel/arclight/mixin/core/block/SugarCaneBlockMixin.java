package io.izzel.arclight.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.SugarCaneBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SugarCaneBlock.class)
public class SugarCaneBlockMixin {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    public boolean arclight$cropGrow(World world, BlockPos pos, BlockState state) {
        return CraftEventFactory.handleBlockGrowEvent(world, pos, state);
    }
}
