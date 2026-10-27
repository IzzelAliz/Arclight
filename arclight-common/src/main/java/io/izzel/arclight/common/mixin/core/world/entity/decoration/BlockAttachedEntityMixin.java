package io.izzel.arclight.common.mixin.core.world.entity.decoration;

import io.izzel.arclight.common.mixin.core.world.entity.EntityMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.entity.Hanging;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockAttachedEntity.class)
public abstract class BlockAttachedEntityMixin extends EntityMixin {

    // @formatter:off
    @Shadow protected BlockPos pos;
    @Shadow public abstract BlockPos getPos();
    // @formatter:on

    @Inject(method = "addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V", cancellable = true, at = @At("HEAD"))
    private void arclight$skipSave(ValueOutput output, CallbackInfo ci) {
        if (this.arclight$saveNotIncludeAll) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/BlockAttachedEntity;discard()V"))
    private void arclight$hangingBreak(CallbackInfo ci) {
        // Modded block-attached entities need not implement Bukkit Hanging.
        if (!(this.getBukkitEntity() instanceof Hanging hanging)) {
            this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DROP);
            return;
        }
        var state = this.level().getBlockState(new BlockPos(this.blockPosition()));
        HangingBreakEvent.RemoveCause cause;
        if (!state.isAir()) {
            cause = HangingBreakEvent.RemoveCause.OBSTRUCTION;
        } else {
            cause = HangingBreakEvent.RemoveCause.PHYSICS;
        }
        HangingBreakEvent event = new HangingBreakEvent(hanging, cause);
        Bukkit.getPluginManager().callEvent(event);
        if (this.isRemoved() || event.isCancelled()) {
            ci.cancel();
        } else {
            this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DROP);
        }
    }

    @Inject(method = "hurtServer", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/BlockAttachedEntity;kill(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void arclight$hangingBreakByAttack(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        // Modded block-attached entities need not implement Bukkit Hanging.
        if (!(this.getBukkitEntity() instanceof Hanging hanging)) {
            return;
        }
        Entity damager = (source.isDirect()) ? source.getDirectEntity() : source.getEntity();
        HangingBreakEvent event;
        if (damager != null) {
            event = new HangingBreakByEntityEvent(hanging, damager.bridge$getBukkitEntity(), new org.bukkit.craftbukkit.damage.CraftDamageSource(source), source.is(DamageTypeTags.IS_EXPLOSION) ? HangingBreakEvent.RemoveCause.EXPLOSION : HangingBreakEvent.RemoveCause.ENTITY);
        } else {
            event = new HangingBreakEvent(hanging, source.is(DamageTypeTags.IS_EXPLOSION) ? HangingBreakEvent.RemoveCause.EXPLOSION : HangingBreakEvent.RemoveCause.DEFAULT);
        }
        Bukkit.getPluginManager().callEvent(event);
        if (this.isRemoved() || event.isCancelled()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "move", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/BlockAttachedEntity;kill(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void arclight$hangingBreakByMove(MoverType typeIn, Vec3 pos, CallbackInfo ci) {
        // Modded block-attached entities need not implement Bukkit Hanging.
        if (!(this.getBukkitEntity() instanceof Hanging hanging)) {
            return;
        }
        if (this.isRemoved()) {
            ci.cancel();
            return;
        }
        HangingBreakEvent event = new HangingBreakEvent(hanging, HangingBreakEvent.RemoveCause.PHYSICS);
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
}
