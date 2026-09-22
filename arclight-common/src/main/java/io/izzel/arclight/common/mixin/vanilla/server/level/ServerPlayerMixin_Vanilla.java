package io.izzel.arclight.common.mixin.vanilla.server.level;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.mixin.vanilla.world.entity.player.PlayerMixin_Vanilla;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.player.PlayerSpawnChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin_Vanilla extends Player implements ServerPlayerBridge {

    protected ServerPlayerMixin_Vanilla(net.minecraft.world.level.Level level, com.mojang.authlib.GameProfile profile) {
        super(level, profile);
    }


    @Inject(method = "startSleepInBed", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setRespawnPosition(Lnet/minecraft/server/level/ServerPlayer$RespawnConfig;Z)V"))
    private void arclight$bedCause(BlockPos p_9115_, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        this.bridge$pushChangeSpawnCause(PlayerSpawnChangeEvent.Cause.BED);
    }

    @Inject(method = "startSleepInBed", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;startSleepInBed(Lnet/minecraft/core/BlockPos;)Lcom/mojang/datafixers/util/Either;"))
    private void arclight$beforeActualSleep(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        Either<Player.BedSleepingProblem, Unit> decision = bridge$fireBedEvent(Either.right(Unit.INSTANCE), pos);
        if (decision.left().isPresent()) {
            cir.setReturnValue(decision);
        } else {
            bridge$markBedEnterEventFired();
        }
    }

    @Redirect(method = "startSleepInBed", require = 0, at = @At(value = "INVOKE", remap = false, target = "Lcom/mojang/datafixers/util/Either;left(Ljava/lang/Object;)Lcom/mojang/datafixers/util/Either;"))
    private <L, R> Either<L, R> arclight$failSleep(L value, BlockPos pos) {
        Either<L, R> decision = bridge$fireBedEvent(Either.left(value), pos);
        if (decision.left().isPresent()) return decision;
        ServerPlayer player = (ServerPlayer) (Object) this;
        Either<Player.BedSleepingProblem, Unit> slept = arclight$invokeParentSleep(pos);
        slept.ifRight(unit -> {
            player.awardStat(net.minecraft.stats.Stats.SLEEP_IN_BED);
            net.minecraft.advancements.CriteriaTriggers.SLEPT_IN_BED.trigger(player);
        });
        if (!player.level().canSleepThroughNights()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("sleep.not_possible"), true);
        }
        player.level().updateSleepingPlayerList();
        return (Either<L, R>) slept;
    }
    private Either<Player.BedSleepingProblem, Unit> arclight$invokeParentSleep(BlockPos pos) {
        return super.startSleepInBed(pos);
    }
}
