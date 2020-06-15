package io.izzel.arclight.common.mixin.v1_15.world.server;

import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import io.izzel.arclight.common.mixin.core.world.WorldMixin;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.event.world.TimeSkipEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin_1_15 extends WorldMixin implements ServerWorldBridge {

    // @formatter:off
    @Shadow private boolean allPlayersSleeping;
    @Shadow @Final private List<ServerPlayerEntity> players;
    @Shadow protected abstract void wakeUpAllPlayers();
    // @formatter:on

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;setDayTime(J)V"))
    private void arclight$timeSkip(ServerWorld world, long time) {
        TimeSkipEvent event = new TimeSkipEvent(this.bridge$getWorld(), TimeSkipEvent.SkipReason.NIGHT_SKIP, (time - time % 24000L) - this.getDayTime());
        Bukkit.getPluginManager().callEvent(event);
        arclight$timeSkipCancelled = event.isCancelled();
        if (!event.isCancelled()) {
            world.setDayTime(this.getDayTime() + event.getSkipAmount());
            this.allPlayersSleeping = this.players.stream().allMatch(LivingEntity::isSleeping);
        }
    }

    private transient boolean arclight$timeSkipCancelled;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;wakeUpAllPlayers()V"))
    private void arclight$notWakeIfCancelled(ServerWorld world) {
        if (!arclight$timeSkipCancelled) {
            this.wakeUpAllPlayers();
        }
        arclight$timeSkipCancelled = false;
    }
}
