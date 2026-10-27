package io.izzel.arclight.common.mixin.core.world.entity.boss.enderdragon.phases;

import com.destroystokyo.paper.event.entity.EnderDragonShootFireballEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonStrafePlayerPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(DragonStrafePlayerPhase.class)
public abstract class DragonStrafePlayerPhaseMixin extends AbstractDragonPhaseInstance {

    protected DragonStrafePlayerPhaseMixin(EnderDragon dragon) {
        super(dragon);
    }

    // Keep both original continuations, but decide/play the sound only after the event.
    // WrapOperation is needed because these continuations escape into invocation-local shares.
    @WrapOperation(method = "doServerTick(Lnet/minecraft/server/level/ServerLevel;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;isSilent()Z"), require = 1)
    private boolean arclight$deferShootSilence(EnderDragon dragon, Operation<Boolean> original,
                                             @Share("arclightDragonShootSilent") LocalRef<BooleanSupplier> silent) {
        silent.set(() -> original.call(dragon));
        // Enter the old sound branch only to capture its continuation, without playing it.
        return false;
    }

    @WrapOperation(method = "doServerTick(Lnet/minecraft/server/level/ServerLevel;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;blockPosition()Lnet/minecraft/core/BlockPos;"), require = 1)
    private BlockPos arclight$deferShootPosition(EnderDragon dragon, Operation<BlockPos> original,
                                               @Share("arclightDragonShootPosition") LocalRef<Supplier<BlockPos>> position) {
        position.set(() -> original.call(dragon));
        // Only the deferred sound consumes this argument; resolve its actual value after the event.
        return BlockPos.ZERO;
    }

    @WrapOperation(method = "doServerTick(Lnet/minecraft/server/level/ServerLevel;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;levelEvent(Lnet/minecraft/world/entity/Entity;ILnet/minecraft/core/BlockPos;I)V"), require = 1)
    private void arclight$deferShootSound(ServerLevel level, Entity excluded, int event, BlockPos pos, int data,
                                         Operation<Void> original,
                                         @Share("arclightDragonShootSilent") LocalRef<BooleanSupplier> silent,
                                         @Share("arclightDragonShootPosition") LocalRef<Supplier<BlockPos>> position,
                                         @Share("arclightDragonShootSound") LocalRef<Runnable> sound) {
        sound.set(() -> {
            if (!silent.get().getAsBoolean()) {
                // A listener can change the dragon's silence and position before the sound.
                original.call(level, excluded, event, position.get().get(), data);
            }
        });
    }

    @WrapOperation(method = "doServerTick(Lnet/minecraft/server/level/ServerLevel;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"), require = 1)
    private boolean arclight$shootFireballEvent(ServerLevel level, Entity fireball, Operation<Boolean> original,
                                               @Share("arclightDragonShootSound") LocalRef<Runnable> sound) {
        var event = new EnderDragonShootFireballEvent((org.bukkit.entity.EnderDragon) this.dragon.bridge$getBukkitEntity(),
            (org.bukkit.entity.DragonFireball) fireball.bridge$getBukkitEntity());
        if (!event.callEvent()) {
            ((EntityBridge) fireball).bridge$pushEntityRemoveCause(null);
            fireball.discard();
            return false;
        }
        sound.get().run();
        // The native caller ignores false and still resets charge/path/phase, as Paper does.
        return original.call(level, fireball);
    }
}
