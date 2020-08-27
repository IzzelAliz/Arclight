package io.izzel.arclight.common.mixin.core.world.raid;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.raid.RaidBridge;
import net.minecraft.advancements.criterion.PositionTrigger;
import net.minecraft.entity.monster.AbstractRaiderEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.raid.Raid;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.raid.RaidStopEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
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
    @Shadow @Final private Map<Integer, Set<AbstractRaiderEntity>> raiders;
    @Shadow @Final private ServerWorld world;
    // @formatter:on

    @Inject(method = "tick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/raid/Raid;stop()V"),
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/Difficulty;PEACEFUL:Lnet/minecraft/world/Difficulty;")))
    public void arclight$stopPeace(CallbackInfo ci) {
        CraftEventFactory.callRaidStopEvent((Raid) (Object) this, RaidStopEvent.Reason.PEACE);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/raid/Raid;stop()V"),
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;isVillage(Lnet/minecraft/util/math/BlockPos;)Z"),
            to = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/raid/Raid;ticksActive:J")
        ))
    public void arclight$stopNotInVillage(CallbackInfo ci) {
        CraftEventFactory.callRaidStopEvent((Raid) (Object) this, RaidStopEvent.Reason.NOT_IN_VILLAGE);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/raid/Raid;stop()V"),
        slice = @Slice(
            from = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/raid/Raid;ticksActive:J"),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/world/raid/Raid;getRaiderCount()I")
        ))
    public void arclight$stopTimeout(CallbackInfo ci) {
        CraftEventFactory.callRaidStopEvent((Raid) (Object) this, RaidStopEvent.Reason.TIMEOUT);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/raid/Raid;stop()V"),
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/world/raid/Raid;shouldSpawnGroup()Z"),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/world/raid/Raid;isStarted()Z")
        ))
    public void arclight$stopUnspawnable(CallbackInfo ci) {
        CraftEventFactory.callRaidStopEvent((Raid) (Object) this, RaidStopEvent.Reason.UNSPAWNABLE);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/raid/Raid;stop()V"),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/raid/Raid;isOver()Z")))
    public void arclight$stopFinish(CallbackInfo ci) {
        CraftEventFactory.callRaidStopEvent((Raid) (Object) this, RaidStopEvent.Reason.FINISHED);
    }

    @Inject(method = "tick", at = @At(value = "FIELD", shift = At.Shift.BY, by = 2, target = "Lnet/minecraft/world/raid/Raid$Status;LOSS:Lnet/minecraft/world/raid/Raid$Status;"))
    public void arclight$finishNone(CallbackInfo ci) {
        CraftEventFactory.callRaidFinishEvent((Raid) (Object) this, new ArrayList<>());
    }

    private transient List<Player> arclight$winners;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/criterion/PositionTrigger;trigger(Lnet/minecraft/entity/player/ServerPlayerEntity;)V"))
    public void arclight$addWinner(PositionTrigger trigger, ServerPlayerEntity player) {
        trigger.trigger(player);
        if (arclight$winners == null) {
            arclight$winners = new ArrayList<>();
        }
        arclight$winners.add(((ServerPlayerEntityBridge) player).bridge$getBukkitEntity());
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/raid/Raid;markDirty()V"))
    public void arclight$finish(CallbackInfo ci) {
        List<Player> winners = this.arclight$winners == null ? new ArrayList<>() : this.arclight$winners;
        this.arclight$winners = null;
        CraftEventFactory.callRaidFinishEvent((Raid) (Object) this, winners);
    }

    private transient AbstractRaiderEntity arclight$leader;
    private transient List<AbstractRaiderEntity> arclight$raiders;

    @Redirect(method = "spawnNextWave", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/raid/Raid;setLeader(ILnet/minecraft/entity/monster/AbstractRaiderEntity;)V"))
    public void arclight$captureLeader(Raid raid, int raidId, AbstractRaiderEntity entity) {
        raid.setLeader(raidId, entity);
        arclight$leader = entity;
    }

    @Redirect(method = "spawnNextWave", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/raid/Raid;joinRaid(ILnet/minecraft/entity/monster/AbstractRaiderEntity;Lnet/minecraft/util/math/BlockPos;Z)V"))
    public void arclight$captureRaider(Raid raid, int wave, AbstractRaiderEntity entity, BlockPos pos, boolean flag) {
        raid.joinRaid(wave, entity, pos, flag);
        if (arclight$raiders == null) {
            arclight$raiders = new ArrayList<>();
        }
        arclight$raiders.add(entity);
    }

    @Inject(method = "spawnNextWave", at = @At("RETURN"))
    public void arclight$spawnWave(BlockPos pos, CallbackInfo ci) {
        CraftEventFactory.callRaidSpawnWaveEvent((Raid) (Object) this, arclight$leader, arclight$raiders);
    }

    @Inject(method = "joinRaid(ILnet/minecraft/entity/monster/AbstractRaiderEntity;Lnet/minecraft/util/math/BlockPos;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;func_242417_l(Lnet/minecraft/entity/Entity;)V"))
    public void arclight$addEntity(int wave, AbstractRaiderEntity p_221317_2_, BlockPos p_221317_3_, boolean p_221317_4_, CallbackInfo ci) {
        ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.RAID);
    }

    public Collection<AbstractRaiderEntity> getRaiders() {
        HashSet<AbstractRaiderEntity> set = new HashSet<>();
        for (Set<AbstractRaiderEntity> entities : this.raiders.values()) {
            set.addAll(entities);
        }
        return set;
    }

    @Override
    public Collection<AbstractRaiderEntity> bridge$getRaiders() {
        return getRaiders();
    }
}
