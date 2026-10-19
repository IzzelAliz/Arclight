package io.izzel.arclight.common.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningBolt.class)
public abstract class LightningBoltMixin extends EntityMixin {

    @Shadow private int life;

    public boolean isSilent = false;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LightningBolt;discard()V"))
    private void arclight$tickDespawn(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Redirect(method = "tick", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, ordinal = 0, target = "Lnet/minecraft/world/entity/LightningBolt;life:I"))
    private int arclight$silent(LightningBolt lightningBolt) {
        return isSilent ? 0 : this.life;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;thunderHit(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LightningBolt;)V"))
    private void arclight$captureEntity(Entity entity, ServerLevel world, LightningBolt lightning, Operation<Void> original) {
        // The damage-event getter consumes the slot; snapshot without consuming parent context.
        Entity previous = ArclightCaptures.peekDamageEventEntity();
        ArclightCaptures.captureDamageEventEntity(lightning);
        try {
            original.call(entity, world, lightning);
        } finally {
            ArclightCaptures.captureDamageEventEntity(previous);
        }
    }

    @WrapOperation(method = "spawnFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"), require = 2, expect = 2, allow = 2)
    private boolean arclight$blockIgnite(ServerLevel world, BlockPos pos, BlockState state, Operation<Boolean> original,
                                        @Share(value = "igniteVeto", namespace = "arclight:lightning") LocalBooleanRef veto) {
        // One decision per candidate, in this spawnFire invocation (also across reentry).
        veto.set(CraftEventFactory.callBlockIgniteEvent(world, pos, (LightningBolt) (Object) this).isCancelled());
        if (veto.get()) {
            return false;
        }
        return original.call(world, pos, state);
    }

    @WrapOperation(method = "spawnFire", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/entity/LightningBolt;blocksSetOnFire:I"), require = 2, expect = 2, allow = 2)
    private void arclight$igniteCredit(LightningBolt bolt, int value, Operation<Void> original,
                                      @Share(value = "igniteVeto", namespace = "arclight:lightning") LocalBooleanRef veto) {
        // Veto skips only this credit; an allowed setter returning false still counts.
        if (!veto.get()) {
            original.call(bolt, value);
        }
    }

}
