package io.izzel.arclight.common.mixin.core.world.entity.raid;

import io.izzel.arclight.common.bridge.core.world.entity.raid.RaidBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(Raid.class)
public class RaidMixin implements RaidBridge {

    // @formatter:off
    @Shadow @Final private Map<Integer, Set<Raider>> groupRaiderMap;
    @Shadow @Final private ServerLevel level;
    // @formatter:on

    private transient Raider arclight$leader;
    private transient List<Raider> arclight$raiders;

    @Decorate(method = "spawnGroup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;setLeader(ILnet/minecraft/world/entity/raid/Raider;)V"))
    public void arclight$captureLeader(Raid raid, int raidId, Raider entity) throws Throwable {
        DecorationOps.callsite().invoke(raid, raidId, entity);
        arclight$leader = entity;
    }

    @Decorate(method = "spawnGroup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;joinRaid(ILnet/minecraft/world/entity/raid/Raider;Lnet/minecraft/core/BlockPos;Z)V"))
    public void arclight$captureRaider(Raid raid, int wave, Raider entity, BlockPos pos, boolean flag) throws Throwable {
        DecorationOps.callsite().invoke(raid, wave, entity, pos, flag);
        if (arclight$raiders == null) {
            arclight$raiders = new ArrayList<>();
        }
        arclight$raiders.add(entity);
    }

    @Inject(method = "spawnGroup", at = @At("RETURN"))
    public void arclight$spawnWave(BlockPos pos, CallbackInfo ci) {
        CraftEventFactory.callRaidSpawnWaveEvent((Raid) (Object) this, this.level, arclight$leader, arclight$raiders);
        arclight$leader = null;
        arclight$raiders = null;
    }

    @Inject(method = "joinRaid(ILnet/minecraft/world/entity/raid/Raider;Lnet/minecraft/core/BlockPos;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    public void arclight$addEntity(int wave, Raider raider, BlockPos pos, boolean flag, CallbackInfo ci) {
        ((WorldBridge) this.level).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.RAID);
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
