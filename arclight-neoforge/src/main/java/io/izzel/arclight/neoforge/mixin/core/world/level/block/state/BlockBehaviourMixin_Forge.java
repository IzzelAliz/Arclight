package io.izzel.arclight.neoforge.mixin.core.world.level.block.state;

import io.izzel.arclight.common.bridge.core.world.level.block.state.BlockBehaviourBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin_Forge implements BlockBehaviourBridge {

    @Override
    public boolean bridge$forge$canDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return state.getBlock().canDropFromExplosion(state, level, pos, explosion);
    }

    @Override
    public void bridge$forge$onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        state.getBlock().onBlockExploded(state, level, pos, explosion);
    }
}
