package io.izzel.arclight.common.mixin.v1_15.entity.player;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mixin.v1_15.entity.LivingEntityMixin_1_15;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin_1_15 extends LivingEntityMixin_1_15 implements PlayerEntityBridge {

    // @formatter:off
    @Shadow public abstract Either<PlayerEntity.SleepResult, Unit> trySleep(BlockPos at);
    @Shadow public abstract void startSleeping(BlockPos p_213342_1_);
    @Shadow public int sleepTimer;
    @Shadow @Final public PlayerAbilities abilities;
    @Shadow public abstract void addStat(ResourceLocation stat);
    // @formatter:on

    @Override
    public CraftHumanEntity bridge$getBukkitEntity() {
        return (CraftHumanEntity) ((InternalEntityBridge) this).internal$getBukkitEntity();
    }

    private boolean arclight$forceSleep = false;
    private Object arclight$processSleep = null;

    private Either<PlayerEntity.SleepResult, Unit> getBedResult(BlockPos at, Direction direction) {
        arclight$processSleep = true;
        Either<PlayerEntity.SleepResult, Unit> either = this.trySleep(at);
        arclight$processSleep = null;
        return either;
    }

    public Either<PlayerEntity.SleepResult, Unit> sleep(BlockPos at, boolean force) {
        arclight$forceSleep = force;
        try {
            return this.trySleep(at);
        } finally {
            arclight$forceSleep = false;
        }
    }

    @Inject(method = "trySleep", cancellable = true, at = @At(value = "HEAD"))
    public void arclight$onSleep(BlockPos at, CallbackInfoReturnable<Either<PlayerEntity.SleepResult, Unit>> cir) {
        if (arclight$processSleep == null) {
            Either<PlayerEntity.SleepResult, Unit> result = getBedResult(at, null);

            if (result.left().orElse(null) == PlayerEntity.SleepResult.OTHER_PROBLEM) {
                cir.setReturnValue(result);
                return;
            }
            if (arclight$forceSleep) {
                result = Either.right(Unit.INSTANCE);
            }
            if (this.bridge$getBukkitEntity() instanceof Player) {
                result = CraftEventFactory.callPlayerBedEnterEvent((PlayerEntity) (Object) this, at, result);
                if (result.left().isPresent()) {
                    cir.setReturnValue(result);
                    return;
                }
            }

            this.startSleeping(at);
            this.sleepTimer = 0;
            if (this.world instanceof ServerWorld) {
                ((ServerWorld) this.world).updateAllPlayersSleepingFlag();
            }
            cir.setReturnValue(Either.right(Unit.INSTANCE));
        }
    }

    @Inject(method = "trySleep", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;startSleeping(Lnet/minecraft/util/math/BlockPos;)V"))
    public void arclight$preSleep(BlockPos at, CallbackInfoReturnable<Either<PlayerEntity.SleepResult, Unit>> cir) {
        if (arclight$processSleep != null) {
            cir.setReturnValue(Either.right(Unit.INSTANCE));
        }
    }

    @Inject(method = "stopSleepInBed", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;sleepTimer:I"))
    private void arclight$wakeup(boolean flag, boolean flag1, CallbackInfo ci) {
        BlockPos blockPos = this.getBedPosition().orElse(null);
        if (this.bridge$getBukkitEntity() instanceof Player) {
            Player player = (Player) this.bridge$getBukkitEntity();
            Block bed;
            if (blockPos != null) {
                bed = CraftBlock.at(this.world, blockPos);
            } else {
                bed = ((WorldBridge) this.world).bridge$getWorld().getBlockAt(player.getLocation());
            }
            PlayerBedLeaveEvent event = new PlayerBedLeaveEvent(player, bed, true);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    @Inject(method = "setSpawnPoint", remap = false, at = @At("RETURN"))
    private void arclight$updateSpawnpoint(BlockPos pos, boolean p_226560_2_, boolean p_226560_3_, DimensionType dim, CallbackInfo ci) {
        bridge$setSpawnWorld(pos == null ? "" : this.world.worldInfo.getWorldName());
    }

    @Inject(method = "startFallFlying", cancellable = true, at = @At("HEAD"))
    private void arclight$startGlidingEvent(CallbackInfo ci) {
        if (CraftEventFactory.callToggleGlideEvent((PlayerEntity) (Object) this, true).isCancelled()) {
            this.setFlag(7, true);
            this.setFlag(7, false);
            ci.cancel();
        }
    }

    @Inject(method = "stopFallFlying", cancellable = true, at = @At("HEAD"))
    private void arclight$stopGlidingEvent(CallbackInfo ci) {
        if (CraftEventFactory.callToggleGlideEvent((PlayerEntity) (Object) this, false).isCancelled()) {
            ci.cancel();
        }
    }

    @Override
    public Either<PlayerEntity.SleepResult, Unit> bridge$trySleep(BlockPos at, boolean force) {
        return sleep(at, force);
    }
}
