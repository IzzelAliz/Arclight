package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.level.portal.TeleportTransitionBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.portal.TeleportTransition;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndGatewayBlock.class)
public class EndGatewayBlockMixin {

    @Inject(method = "getPortalDestination", at = @At("RETURN"))
    private void arclight$setCause(ServerLevel serverLevel, Entity entity, BlockPos blockPos, CallbackInfoReturnable<TeleportTransition> cir) {
        var dimensionTransition = cir.getReturnValue();
        if (dimensionTransition != null) {
            ((TeleportTransitionBridge) ((Object) dimensionTransition)).bridge$setTeleportCause(PlayerTeleportEvent.TeleportCause.END_GATEWAY);
        }
    }
}
