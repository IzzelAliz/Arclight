package io.izzel.arclight.impl.mixin.v1_14.entity.player;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.entity.LivingEntityMixin;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Optional;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin_1_14 extends LivingEntityMixin implements PlayerEntityBridge {

    // @formatter:off
    @Shadow public abstract Either<PlayerEntity.SleepResult, Unit> trySleep(BlockPos at);
    @Shadow public abstract void startSleeping(BlockPos p_213342_1_);
    @Shadow public int sleepTimer;
    @Shadow public abstract void addStat(ResourceLocation stat);
    @Shadow @Final public PlayerAbilities abilities;
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

    @Inject(method = "wakeUpPlayer", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;sleepTimer:I"))
    private void arclight$wakeup(boolean immediately, boolean updateWorldFlag, boolean setSpawn, CallbackInfo ci, Optional<BlockPos> optional) {
        if (this.getBukkitEntity() instanceof Player) {
            Player player = (Player) this.getBukkitEntity();
            BlockPos blockposition2 = optional.orElse(null);
            Block bed;
            if (blockposition2 != null) {
                bed = CraftBlock.at(this.world, blockposition2);
            } else {
                bed = ((WorldBridge) this.world).bridge$getWorld().getBlockAt(player.getLocation());
            }
            PlayerBedLeaveEvent event = new PlayerBedLeaveEvent(player, bed, setSpawn);
            Bukkit.getPluginManager().callEvent(event);
            arclight$setSpawn = event.shouldSetSpawnLocation();
        }
    }

    private boolean arclight$setSpawn;

    @ModifyVariable(method = "wakeUpPlayer", index = 3, name = "setSpawn", at = @At(value = "FIELD", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/player/PlayerEntity;sleepTimer:I"))
    private boolean arclight$modifySpawn(boolean setSpawn) {
        return arclight$setSpawn;
    }

    @Inject(method = "setSpawnPoint(Lnet/minecraft/util/math/BlockPos;Z)V", at = @At("RETURN"))
    private void arclight$updateSpawnpoint(BlockPos pos, boolean forced, CallbackInfo ci) {
        bridge$setSpawnWorld(pos == null ? "" : this.world.worldInfo.getWorldName());
    }

    @Override
    public Either<PlayerEntity.SleepResult, Unit> bridge$trySleep(BlockPos at, boolean force) {
        return sleep(at, force);
    }
}
