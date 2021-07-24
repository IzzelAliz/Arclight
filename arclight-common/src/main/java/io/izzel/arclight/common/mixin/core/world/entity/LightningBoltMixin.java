package io.izzel.arclight.common.mixin.core.world.entity;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
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

    @Redirect(method = "tick", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, ordinal = 0, target = "Lnet/minecraft/world/entity/LightningBolt;life:I"))
    private int arclight$silent(LightningBolt lightningBolt) {
        return isSilent ? 0 : this.life;
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;thunderHit(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LightningBolt;)V"))
    private void arclight$captureEntity(CallbackInfo ci) {
        ArclightCaptures.captureDamageEventEntity((Entity) (Object) this);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/Entity;thunderHit(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LightningBolt;)V"))
    private void arclight$resetEntity(CallbackInfo ci) {
        ArclightCaptures.captureDamageEventEntity(null);
    }

    @Redirect(method = "spawnFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean arclight$blockIgnite(Level world, BlockPos pos, BlockState state) {
        if (!CraftEventFactory.callBlockIgniteEvent(world, pos, (LightningBolt) (Object) this).isCancelled()) {
            return world.setBlockAndUpdate(pos, state);
        } else {
            return false;
        }
    }
}
