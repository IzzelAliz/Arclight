package io.izzel.arclight.common.mixin.core.world.entity.item;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.util.IndirectEntityDamageSourceBridge;
import io.izzel.arclight.common.mixin.core.world.entity.EntityMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.IndirectEntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.entity.Hanging;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HangingEntity.class)
public abstract class HangingEntityMixin extends EntityMixin {

    // @formatter:off
    @Shadow public BlockPos pos;
    // @formatter:on

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/HangingEntity;discard()V"))
    private void arclight$hangingBreak(CallbackInfo ci) {
        Material material = this.level.getBlockState(new BlockPos(this.blockPosition())).getMaterial();
        HangingBreakEvent.RemoveCause cause;
        if (!material.equals(Material.AIR)) {
            cause = HangingBreakEvent.RemoveCause.OBSTRUCTION;
        } else {
            cause = HangingBreakEvent.RemoveCause.PHYSICS;
        }
        HangingBreakEvent event = new HangingBreakEvent((Hanging) this.getBukkitEntity(), cause);
        Bukkit.getPluginManager().callEvent(event);
        if (this.isRemoved() || event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "hurt", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/HangingEntity;kill()V"))
    private void arclight$hangingBreakByAttack(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity damager = (source instanceof IndirectEntityDamageSource) ? ((IndirectEntityDamageSourceBridge) source).bridge$getProximateDamageSource() : source.getEntity();
        HangingBreakEvent event;
        if (damager != null) {
            event = new HangingBreakByEntityEvent((Hanging) this.getBukkitEntity(), ((EntityBridge) damager).bridge$getBukkitEntity(), source.isExplosion() ? HangingBreakEvent.RemoveCause.EXPLOSION : HangingBreakEvent.RemoveCause.ENTITY);
        } else {
            event = new HangingBreakEvent((Hanging) this.getBukkitEntity(), source.isExplosion() ? HangingBreakEvent.RemoveCause.EXPLOSION : HangingBreakEvent.RemoveCause.DEFAULT);
        }
        Bukkit.getPluginManager().callEvent(event);
        if (this.isRemoved() || event.isCancelled()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "move", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/HangingEntity;kill()V"))
    private void arclight$hangingBreakByMove(MoverType typeIn, Vec3 pos, CallbackInfo ci) {
        if (this.isRemoved()) {
            ci.cancel();
            return;
        }
        HangingBreakEvent event = new HangingBreakEvent((Hanging) this.getBukkitEntity(), HangingBreakEvent.RemoveCause.PHYSICS);
        Bukkit.getPluginManager().callEvent(event);
        if (this.isRemoved() || event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "push", cancellable = true, at = @At("HEAD"))
    private void arclight$noVelocity(double x, double y, double z, CallbackInfo ci) {
        ci.cancel();
    }

    private static double a(int i) {
        return i % 32 == 0 ? 0.5D : 0.0D;
    }

    private static AABB calculateBoundingBox(Entity entity, BlockPos blockPosition, Direction direction, int width, int height) {
        double d0 = blockPosition.getX() + 0.5;
        double d2 = blockPosition.getY() + 0.5;
        double d3 = blockPosition.getZ() + 0.5;
        double d4 = 0.46875;
        double d5 = a(width);
        double d6 = a(height);
        d0 -= direction.getStepX() * 0.46875;
        d3 -= direction.getStepZ() * 0.46875;
        d2 += d6;
        Direction enumdirection = direction.getCounterClockWise();
        d0 += d5 * enumdirection.getStepX();
        d3 += d5 * enumdirection.getStepZ();
        if (entity != null) {
            entity.setPosRaw(d0, d2, d3);
        }
        double d7 = width;
        double d8 = height;
        double d9 = width;
        if (direction.getAxis() == Direction.Axis.Z) {
            d9 = 1.0;
        } else {
            d7 = 1.0;
        }
        d7 /= 32.0;
        d8 /= 32.0;
        d9 /= 32.0;
        return new AABB(d0 - d7, d2 - d8, d3 - d9, d0 + d7, d2 + d8, d3 + d9);
    }
}
