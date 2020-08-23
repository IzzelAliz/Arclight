package io.izzel.arclight.common.mixin.core.entity.item;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.util.IndirectEntityDamageSourceBridge;
import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.HangingEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.IndirectEntityDamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
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
    @Shadow public BlockPos hangingPosition;
    // @formatter:on

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/HangingEntity;remove()V"))
    private void arclight$hangingBreak(CallbackInfo ci) {
        Material material = this.world.getBlockState(new BlockPos(this.getPosition())).getMaterial();
        HangingBreakEvent.RemoveCause cause;
        if (!material.equals(Material.AIR)) {
            cause = HangingBreakEvent.RemoveCause.OBSTRUCTION;
        } else {
            cause = HangingBreakEvent.RemoveCause.PHYSICS;
        }
        HangingBreakEvent event = new HangingBreakEvent((Hanging) this.getBukkitEntity(), cause);
        Bukkit.getPluginManager().callEvent(event);
        if (this.removed || event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "attackEntityFrom", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/HangingEntity;remove()V"))
    private void arclight$hangingBreakByAttack(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity damager = (source instanceof IndirectEntityDamageSource) ? ((IndirectEntityDamageSourceBridge) source).bridge$getProximateDamageSource() : source.getTrueSource();
        HangingBreakEvent event;
        if (damager != null) {
            event = new HangingBreakByEntityEvent((Hanging) this.getBukkitEntity(), ((EntityBridge) damager).bridge$getBukkitEntity(), source.isExplosion() ? HangingBreakEvent.RemoveCause.EXPLOSION : HangingBreakEvent.RemoveCause.ENTITY);
        } else {
            event = new HangingBreakEvent((Hanging) this.getBukkitEntity(), source.isExplosion() ? HangingBreakEvent.RemoveCause.EXPLOSION : HangingBreakEvent.RemoveCause.DEFAULT);
        }
        Bukkit.getPluginManager().callEvent(event);
        if (this.removed || event.isCancelled()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "move", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/HangingEntity;remove()V"))
    private void arclight$hangingBreakByMove(MoverType typeIn, Vector3d pos, CallbackInfo ci) {
        if (this.removed) {
            ci.cancel();
            return;
        }
        HangingBreakEvent event = new HangingBreakEvent((Hanging) this.getBukkitEntity(), HangingBreakEvent.RemoveCause.PHYSICS);
        Bukkit.getPluginManager().callEvent(event);
        if (this.removed || event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "addVelocity", cancellable = true, at = @At("HEAD"))
    private void arclight$noVelocity(double x, double y, double z, CallbackInfo ci) {
        ci.cancel();
    }

    private static double a(int i) {
        return i % 32 == 0 ? 0.5D : 0.0D;
    }

    private static AxisAlignedBB calculateBoundingBox(Entity entity, BlockPos blockPosition, Direction direction, int width, int height) {
        double d0 = blockPosition.getX() + 0.5;
        double d2 = blockPosition.getY() + 0.5;
        double d3 = blockPosition.getZ() + 0.5;
        double d4 = 0.46875;
        double d5 = a(width);
        double d6 = a(height);
        d0 -= direction.getXOffset() * 0.46875;
        d3 -= direction.getZOffset() * 0.46875;
        d2 += d6;
        Direction enumdirection = direction.rotateYCCW();
        d0 += d5 * enumdirection.getXOffset();
        d3 += d5 * enumdirection.getZOffset();
        if (entity != null) {
            entity.setRawPosition(d0, d2, d3);
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
        return new AxisAlignedBB(d0 - d7, d2 - d8, d3 - d9, d0 + d7, d2 + d8, d3 + d9);
    }
}
