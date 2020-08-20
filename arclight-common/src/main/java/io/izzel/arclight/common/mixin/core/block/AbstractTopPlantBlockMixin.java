package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.AbstractTopPlantBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(AbstractTopPlantBlock.class)
public class AbstractTopPlantBlockMixin {

    @Redirect(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    private boolean arclight$blockGrow(ServerWorld world, BlockPos to, BlockState state, BlockState state1, ServerWorld worldIn, BlockPos from, Random random) {
        return CraftEventFactory.handleBlockSpreadEvent(world, from, to, state);
    }
}
