package io.izzel.arclight.forge.mixin.core.server.level;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
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
public abstract class ServerPlayerMixin_Forge extends io.izzel.arclight.forge.mixin.core.world.entity.player.PlayerMixin_Forge implements ServerPlayerBridge {

    @Inject(method = "startSleepInBed", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setRespawnPosition(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/BlockPos;FZZ)V"))
    private void arclight$bedCause(BlockPos p_9115_, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        this.bridge$pushChangeSpawnCause(PlayerSpawnChangeEvent.Cause.BED);
    }

    @Redirect(method = "startSleepInBed", require = 0, at = @At(value = "INVOKE", remap = false, target = "Lcom/mojang/datafixers/util/Either;left(Ljava/lang/Object;)Lcom/mojang/datafixers/util/Either;"))
    private <L, R> Either<L, R> arclight$failSleep(L value, BlockPos pos) {
        Either<L, R> either = Either.left(value);
        return bridge$fireBedEvent(either, pos);
    }
}
