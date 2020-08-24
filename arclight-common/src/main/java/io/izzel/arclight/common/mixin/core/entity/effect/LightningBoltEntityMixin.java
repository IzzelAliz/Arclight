package io.izzel.arclight.common.mixin.core.entity.effect;

import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningBoltEntity.class)
public abstract class LightningBoltEntityMixin extends EntityMixin {

    public boolean isSilent = false;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;func_241841_a(Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/entity/effect/LightningBoltEntity;)V"))
    private void arclight$captureEntity(CallbackInfo ci) {
        ArclightCaptures.captureDamageEventEntity((Entity) (Object) this);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/Entity;func_241841_a(Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/entity/effect/LightningBoltEntity;)V"))
    private void arclight$resetEntity(CallbackInfo ci) {
        ArclightCaptures.captureDamageEventEntity(null);
    }

    @Redirect(method = "igniteBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    private boolean arclight$blockIgnite(World world, BlockPos pos, BlockState state) {
        if (!CraftEventFactory.callBlockIgniteEvent(world, pos, (LightningBoltEntity) (Object) this).isCancelled()) {
            return world.setBlockState(pos, state);
        } else {
            return false;
        }
    }
}
