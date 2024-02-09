package io.izzel.arclight.neoforge.mixin.core.server.management;

import io.izzel.arclight.common.bridge.core.server.management.PlayerInteractionManagerBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.bukkit.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin_NeoForge implements PlayerInteractionManagerBridge {

    @Inject(method = "destroyBlock", remap = true, at = @At(value = "INVOKE", remap = false, target = "Lnet/neoforged/neoforge/common/CommonHooks;onBlockBreakEvent(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/GameType;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;)I"))
    public void arclight$beforePrimaryEventFired(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ArclightCaptures.captureNextBlockBreakEventAsPrimaryEvent();
    }

    @Inject(method = "destroyBlock", remap = true, at = @At(value = "INVOKE_ASSIGN", remap = false, target = "Lnet/neoforged/neoforge/common/CommonHooks;onBlockBreakEvent(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/GameType;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;)I"))
    public void arclight$handleSecondaryBlockBreakEvents(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ArclightCaptures.BlockBreakEventContext breakEventContext = ArclightCaptures.popSecondaryBlockBreakEvent();
        while (breakEventContext != null) {
            Block block = breakEventContext.getEvent().getBlock();
            bridge$handleBlockDrop(breakEventContext, new BlockPos(block.getX(), block.getY(), block.getZ()));
            breakEventContext = ArclightCaptures.popSecondaryBlockBreakEvent();
        }
    }
}
