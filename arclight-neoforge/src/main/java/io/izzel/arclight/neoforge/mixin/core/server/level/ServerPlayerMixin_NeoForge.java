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

    @Inject(method = "lambda$startSleepInBed$0(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/attribute/BedRule;Lnet/minecraft/core/BlockPos;Z)Lcom/mojang/datafixers/util/Either;", require = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setRespawnPosition(Lnet/minecraft/server/level/ServerPlayer$RespawnConfig;Z)V"))
    private void arclight$bedCause(net.minecraft.world.level.block.state.BlockState bedBlockState, net.minecraft.world.attribute.BedRule rule, BlockPos pos, boolean canSetSpawn, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        this.bridge$pushChangeSpawnCause(PlayerSpawnChangeEvent.Cause.BED);
    }

    @Decorate(method = "startSleepInBed", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/event/EventHooks;canPlayerStartSleeping(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lcom/mojang/datafixers/util/Either;)Lcom/mojang/datafixers/util/Either;"))
    private Either<Player.BedSleepingProblem, Unit> arclight$bedEnterAfterNeoForge(ServerPlayer player, BlockPos pos, Either<Player.BedSleepingProblem, Unit> vanillaResult) throws Throwable {
        Either<Player.BedSleepingProblem, Unit> neoForgeResult = (Either<Player.BedSleepingProblem, Unit>) DecorationOps.callsite().invoke(player, pos, vanillaResult);
        Either<Player.BedSleepingProblem, Unit> result = this.bridge$fireBedEvent(neoForgeResult, pos);
        if (result.right().isPresent()) {
            this.bridge$markBedEnterEventFired();
        }
        return result;
    }
    @io.izzel.arclight.mixin.Decorate(method = "drop(Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;drop(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private net.minecraft.world.entity.item.ItemEntity arclight$recordSelectedDrop(ServerPlayer player, net.minecraft.world.item.ItemStack stack, boolean thrownFromHand, net.minecraft.util.Prediction prediction) throws Throwable {
        var dropped = (net.minecraft.world.entity.item.ItemEntity) io.izzel.arclight.mixin.DecorationOps.callsite().invoke(player, stack, thrownFromHand, prediction);
        ((ServerPlayerBridge) player).bridge$recordSelectedDrop(dropped);
        return dropped;
    }
}
