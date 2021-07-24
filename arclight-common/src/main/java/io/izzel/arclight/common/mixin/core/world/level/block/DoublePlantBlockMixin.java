package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DoublePlantBlock.class)
public class DoublePlantBlockMixin {

    @Inject(method = "playerWillDestroy", cancellable = true, at = @At("HEAD"))
    public void arclight$blockPhysics(Level worldIn, BlockPos pos, BlockState state, Player player, CallbackInfo ci) {
        if (CraftEventFactory.callBlockPhysicsEvent(worldIn, pos).isCancelled()) {
            ci.cancel();
        }
    }
}
