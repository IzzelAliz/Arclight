package io.izzel.arclight.common.mixin.core.world.entity.raid;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.izzel.arclight.common.bridge.core.world.entity.raid.RaidBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.raid.RaidStopEvent;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(Raid.class)
public class RaidMixin implements RaidBridge {

    // @formatter:off
    @Shadow @Final private Map<Integer, Set<Raider>> groupRaiderMap;
    // @formatter:on

    // Both real target JARs index this array in normal and bonus branches. Preserve the
    // selected index and native behavior for NeoForge types without a CB growth curve.
    @Redirect(method = "getDefaultNumSpawns", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/raid/Raid$RaiderType;spawnsPerWaveBeforeBonus:[I", args = "array=get"))
    private int arclight$defaultSpawnsForWave(int[] counts, int wave, @org.spongepowered.asm.mixin.injection.Coerce Object type, int wav, boolean isBonusWave) {
        return io.izzel.arclight.common.mod.util.RaidWaveSpawns.count((Enum<?>) type, counts, wave);
    }

    // Tick-only callsites: CraftRaid.stop() owns PLUGIN separately.
    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;stop()V", ordinal = 0))
    private void arclight$peace(ServerLevel level, CallbackInfo ci) {
        CraftEventFactory.callRaidStopEvent((Raid) (Object) this, level, RaidStopEvent.Reason.PEACE);
    }

    // Tick-only callsites: CraftRaid.stop() owns PLUGIN separately.
    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;stop()V", ordinal = 1))
    private void arclight$notInVillage(ServerLevel level, CallbackInfo ci) {
        CraftEventFactory.callRaidStopEvent((Raid) (Object) this, level, RaidStopEvent.Reason.NOT_IN_VILLAGE);
    }

    // Tick-only callsites: CraftRaid.stop() owns PLUGIN separately.
    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;stop()V", ordinal = 2))
    private void arclight$timeout(ServerLevel level, CallbackInfo ci) {
        CraftEventFactory.callRaidStopEvent((Raid) (Object) this, level, RaidStopEvent.Reason.TIMEOUT);
    }

    // Tick-only callsites: CraftRaid.stop() owns PLUGIN separately.
    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;stop()V", ordinal = 3))
    private void arclight$unspawnable(ServerLevel level, CallbackInfo ci) {
        CraftEventFactory.callRaidStopEvent((Raid) (Object) this, level, RaidStopEvent.Reason.UNSPAWNABLE);
    }

    // Tick-only callsites: CraftRaid.stop() owns PLUGIN separately.
    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;stop()V", ordinal = 4))
    private void arclight$finished(ServerLevel level, CallbackInfo ci) {
        CraftEventFactory.callRaidStopEvent((Raid) (Object) this, level, RaidStopEvent.Reason.FINISHED);
    }

    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/raid/Raid;status:Lnet/minecraft/world/entity/raid/Raid$RaidStatus;", opcode = 181, ordinal = 0, shift = At.Shift.AFTER))
    private void arclight$loss(ServerLevel level, CallbackInfo ci) {
        CraftEventFactory.callRaidFinishEvent((Raid) (Object) this, level, new ArrayList<>());
    }

    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/raid/Raid;status:Lnet/minecraft/world/entity/raid/Raid$RaidStatus;", opcode = 181, ordinal = 1, shift = At.Shift.AFTER))
    private void arclight$victoryStart(ServerLevel level, CallbackInfo ci, @Share("raidWinners") LocalRef<List<Player>> winners) {
        winners.set(new ArrayList<>());
    }

    // Capture the original iteration's player only after both stat and criterion.
    // Do not requery heroes, filter again, or include living non-player heroes.
    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/criterion/PlayerTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;)V", shift = At.Shift.AFTER))
    private void arclight$victoryPlayer(ServerLevel level, CallbackInfo ci, @Local ServerPlayer player, @Share("raidWinners") LocalRef<List<Player>> winners) {
        winners.get().add((Player) ((io.izzel.arclight.common.bridge.core.entity.EntityBridge) player).bridge$getBukkitEntity());
    }

    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;setDirty(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void arclight$victoryFinish(ServerLevel level, CallbackInfo ci, @Share("raidWinners") LocalRef<List<Player>> winners) {
        if (winners.get() != null) {
            CraftEventFactory.callRaidFinishEvent((Raid) (Object) this, level, winners.get());
        }
    }

    @Inject(method = "spawnGroup(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"))
    private void arclight$waveStart(ServerLevel level, BlockPos pos, CallbackInfo ci, @Share("waveRaiders") LocalRef<List<Raider>> raiders) {
        raiders.set(new ArrayList<>());
    }

    @Inject(method = "spawnGroup(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;setLeader(ILnet/minecraft/world/entity/raid/Raider;)V", shift = At.Shift.AFTER))
    private void arclight$waveLeader(ServerLevel level, BlockPos pos, CallbackInfo ci, @Local(ordinal = 0) Raider raider, @Share("waveLeader") LocalRef<Raider> leader) {
        leader.set(raider);
    }

    @Inject(method = "spawnGroup(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;joinRaid(Lnet/minecraft/server/level/ServerLevel;ILnet/minecraft/world/entity/raid/Raider;Lnet/minecraft/core/BlockPos;Z)V", ordinal = 0, shift = At.Shift.AFTER))
    private void arclight$waveRaider(ServerLevel level, BlockPos pos, CallbackInfo ci, @Local(ordinal = 0) Raider raider, @Share("waveRaiders") LocalRef<List<Raider>> raiders) {
        raiders.get().add(raider);
    }

    // Fixed CB intentionally appends the host again, not the rider, and does not
    // filter by add success. List policy changes belong to a separate contract.
    @Inject(method = "spawnGroup(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raider;startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", shift = At.Shift.AFTER))
    private void arclight$waveRavager(ServerLevel level, BlockPos pos, CallbackInfo ci, @Local(ordinal = 0) Raider raider, @Share("waveRaiders") LocalRef<List<Raider>> raiders) {
        raiders.get().add(raider);
    }

    @Inject(method = "spawnGroup(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V", at = @At("RETURN"))
    private void arclight$waveFinish(ServerLevel level, BlockPos pos, CallbackInfo ci, @Share("waveLeader") LocalRef<Raider> leader, @Share("waveRaiders") LocalRef<List<Raider>> raiders) {
        CraftEventFactory.callRaidSpawnWaveEvent((Raid) (Object) this, level, leader.get(), raiders.get());
    }

    @WrapOperation(method = "joinRaid(Lnet/minecraft/server/level/ServerLevel;ILnet/minecraft/world/entity/raid/Raider;Lnet/minecraft/core/BlockPos;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$raidSpawnReason(ServerLevel level, Entity entity, Operation<Void> original) {
        WorldBridge world = (WorldBridge) level;
        CreatureSpawnEvent.SpawnReason previous = world.bridge$getAddEntityReason();
        world.bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.RAID);
        try {
            original.call(level, entity);
        } finally {
            world.bridge$pushAddEntityReason(previous);
        }
    }

    public Collection<Raider> getRaiders() {
        HashSet<Raider> set = new HashSet<>();
        for (Set<Raider> entities : this.groupRaiderMap.values()) {
            set.addAll(entities);
        }
        return set;
    }

    @Override
    public Collection<Raider> bridge$getRaiders() {
        return getRaiders();
    }
}
