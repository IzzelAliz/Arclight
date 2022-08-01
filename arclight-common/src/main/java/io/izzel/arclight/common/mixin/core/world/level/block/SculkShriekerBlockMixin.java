package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(SculkShriekerBlock.class)
public class SculkShriekerBlockMixin {

    @Inject(method = "stepOn", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntityType;)Ljava/util/Optional;"))
    private void arclight$interact(Level p_222177_, BlockPos pos, BlockState p_222179_, Entity p_222180_, CallbackInfo ci,
                                   ServerLevel level, ServerPlayer player) {
        if (CraftEventFactory.callPlayerInteractEvent(player, org.bukkit.event.block.Action.PHYSICAL, pos, null, null, null).isCancelled()) {
            ci.cancel();
        }
    }
}
