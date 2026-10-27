package io.izzel.arclight.common.mixin.core.world.entity.animal.horse;

import com.destroystokyo.paper.event.entity.SkeletonHorseTrapEvent;
import com.google.common.collect.ImmutableList;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerLevelBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.SkeletonTrapGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SkeletonTrapGoal.class)
public class SkeletonTrapGoalMixin {

    // @formatter:off
    @Shadow @Final private SkeletonHorse horse;
    // @formatter:on

    @Unique private List<HumanEntity> arclight$eligiblePlayers = ImmutableList.of();

    @Decorate(method = "canUse()Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;hasNearbyAlivePlayer(DDDD)Z"), require = 1, allow = 1)
    private boolean arclight$eligiblePlayers(Level level, double x, double y, double z, double range) throws Throwable {
        boolean canUse = (boolean) DecorationOps.callsite().invoke(level, x, y, z, range);
        ImmutableList.Builder<HumanEntity> players = ImmutableList.builder();
        if (canUse) {
            for (Player player : level.players()) {
                if (EntitySelector.NO_SPECTATORS.test(player) && EntitySelector.LIVING_ENTITY_STILL_ALIVE.test(player)
                    && (range < 0.0 || player.distanceToSqr(x, y, z) < range * range)) {
                    players.add((HumanEntity) ((EntityBridge) player).bridge$getBukkitEntity());
                }
            }
        }
        // Keep the canUse snapshot: NeoForge defers the trap body to a scheduled lambda.
        this.arclight$eligiblePlayers = players.build();
        return canUse;
    }

    @Decorate(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/equine/SkeletonHorse;blockPosition()Lnet/minecraft/core/BlockPos;"), require = 1, allow = 1)
    private BlockPos arclight$trapEvent(SkeletonHorse horse) throws Throwable {
        SkeletonHorseTrapEvent event = new SkeletonHorseTrapEvent(
            (org.bukkit.entity.SkeletonHorse) ((EntityBridge) this.horse).bridge$getBukkitEntity(), this.arclight$eligiblePlayers);
        if (!event.callEvent()) {
            // Both native trap hosts return void; cancel before difficulty lookup or horse mutations.
            return (BlockPos) DecorationOps.cancel().invoke();
        }
        return (BlockPos) DecorationOps.callsite().invoke(horse);
    }

    @Inject(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"),
        slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/equine/SkeletonTrapGoal;createHorse(Lnet/minecraft/world/DifficultyInstance;)Lnet/minecraft/world/entity/animal/equine/AbstractHorse;")))
    private void arclight$thunder(CallbackInfo ci) {
        ((WorldBridge) this.horse.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.TRAP);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$addHorse(ServerLevel world, Entity entityIn) {
        ((ServerLevelBridge) world).bridge$strikeLightning((LightningBolt) entityIn, LightningStrikeEvent.Cause.TRAP);
        return true;
    }

    @Inject(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/equine/SkeletonTrapGoal;createHorse(Lnet/minecraft/world/DifficultyInstance;)Lnet/minecraft/world/entity/animal/equine/AbstractHorse;")))
    private void arclight$jockey(CallbackInfo ci) {
        ((WorldBridge) this.horse.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.JOCKEY);
    }
}
