package io.izzel.arclight.neoforge.mixin.core.server.level;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.server.players.PlayerListBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.bukkit.event.player.PlayerSpawnChangeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin_NeoForge extends io.izzel.arclight.neoforge.mixin.core.world.entity.player.PlayerMixin_NeoForge implements ServerPlayerBridge {

    // @formatter:off
    @Shadow @Final public MinecraftServer server;
    // @formatter:on

    @Unique private ResourceKey<Level> arclight$neoforge$fromDimension;

    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z", at = @At("HEAD"))
    private void arclight$neoforge$captureFromDimension(ServerLevel level, double x, double y, double z, Set<RelativeMovement> relativeMovements, float yaw, float pitch, CallbackInfoReturnable<Boolean> cir) {
        this.arclight$neoforge$fromDimension = ((ServerPlayer) (Object) this).serverLevel().dimension();
    }

    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z", at = @At("RETURN"))
    private void arclight$neoforge$firePlayerChangedDimensionEvent(ServerLevel level, double x, double y, double z, Set<RelativeMovement> relativeMovements, float yaw, float pitch, CallbackInfoReturnable<Boolean> cir) {
        ResourceKey<Level> fromDimension = this.arclight$neoforge$fromDimension;
        this.arclight$neoforge$fromDimension = null;
        if (fromDimension == null || !cir.getReturnValueZ()) {
            return;
        }
        ServerPlayer player = (ServerPlayer) (Object) this;
        ResourceKey<Level> toDimension = player.serverLevel().dimension();
        if (!fromDimension.equals(toDimension)) {
            ((PlayerListBridge) this.server.getPlayerList()).bridge$platform$onPlayerChangedDimension(player, fromDimension, toDimension);
        }
    }

    @Inject(method = "lambda$startSleepInBed$13", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setRespawnPosition(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/BlockPos;FZZ)V"))
    private void arclight$bedCause(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        this.bridge$pushChangeSpawnCause(PlayerSpawnChangeEvent.Cause.BED);
    }

    @Redirect(method = "lambda$startSleepInBed$13", require = 0, at = @At(value = "INVOKE", remap = false, target = "Lcom/mojang/datafixers/util/Either;left(Ljava/lang/Object;)Lcom/mojang/datafixers/util/Either;"))
    private <L, R> Either<L, R> arclight$failSleep(L value, BlockPos pos) {
        Either<L, R> either = Either.left(value);
        return bridge$fireBedEvent(either, pos);
    }
}
