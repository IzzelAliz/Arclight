package io.izzel.arclight.neoforge.mixin.core.server.level;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.player.PlayerSpawnChangeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin_NeoForge extends io.izzel.arclight.neoforge.mixin.core.world.entity.player.PlayerMixin_NeoForge implements ServerPlayerBridge {

    // @formatter:off
    @Shadow @Final public MinecraftServer server;
    // @formatter:on

    @Inject(method = "lambda$startSleepInBed$15", require = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setRespawnPosition(Lnet/minecraft/server/level/ServerPlayer$RespawnConfig;Z)V"))
    private void arclight$bedCause(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        this.bridge$pushChangeSpawnCause(PlayerSpawnChangeEvent.Cause.BED);
    }

    @Decorate(method = "startSleepInBed", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/event/EventHooks;canPlayerStartSleeping(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lcom/mojang/datafixers/util/Either;)Lcom/mojang/datafixers/util/Either;"))
    private Either<Player.BedSleepingProblem, Unit> arclight$bedEnterBeforeNeoForge(ServerPlayer player, BlockPos pos, Either<Player.BedSleepingProblem, Unit> vanillaResult) throws Throwable {
        Either<Player.BedSleepingProblem, Unit> bukkitResult = this.bridge$fireBedEvent(vanillaResult, pos);
        Either<Player.BedSleepingProblem, Unit> result = (Either<Player.BedSleepingProblem, Unit>) DecorationOps.callsite().invoke(player, pos, bukkitResult);
        if (result.right().isPresent()) {
            this.bridge$markBedEnterEventFired();
        }
        return result;
    }
}
