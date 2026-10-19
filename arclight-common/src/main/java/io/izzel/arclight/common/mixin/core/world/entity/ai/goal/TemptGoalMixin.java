package io.izzel.arclight.common.mixin.core.world.entity.ai.goal;

import io.izzel.arclight.common.bridge.core.world.entity.ai.goal.ForNonPathfindersTemptBridge;
import io.izzel.arclight.common.bridge.core.world.entity.ai.goal.TemptGoalBridge;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TemptGoal.class)
public abstract class TemptGoalMixin implements TemptGoalBridge {

    // Keep the native Player field descriptor as legacy ABI/projection only.
    // @formatter:off
    @Shadow protected Player player;
    @Shadow @Final protected Mob mob;
    @Shadow @Final protected double speedModifier;
    @Shadow @Final private double stopDistance;
    @Shadow @Final private boolean canScare;
    @Shadow private double px;
    @Shadow private double py;
    @Shadow private double pz;
    @Shadow private double pRotX;
    @Shadow private double pRotY;
    @Shadow private boolean isRunning;
    @Shadow public abstract boolean canUse();
    @Shadow protected abstract boolean canScare();
    @Shadow protected abstract void stopNavigation();
    @Shadow protected abstract void navigateTowards(Player player);
    // @formatter:on

    @Unique private @Nullable LivingEntity arclight$effectiveTarget;

    @Unique
    public final @Nullable LivingEntity arclight$getEffectiveTarget() {
        return this.arclight$effectiveTarget;
    }

    @Unique
    public final boolean arclight$baseCanScare() {
        return this.canScare;
    }

    @Unique
    public final int arclight$adjustedTickDelay(int ticks) {
        return ((Goal) (Object) this).requiresUpdateEveryTick() ? ticks : Mth.positiveCeilDiv(ticks, 2);
    }

    @Unique
    public final Mob arclight$getTemptMob() {
        return this.mob;
    }

    @Unique
    public final double arclight$getTemptSpeedModifier() {
        return this.speedModifier;
    }

    @Inject(method = "canUse", cancellable = true, at = @At(value = "FIELD", shift = At.Shift.AFTER, opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/entity/ai/goal/TemptGoal;player:Lnet/minecraft/world/entity/player/Player;"))
    private void arclight$tempt(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = null;
        if (this.player != null) {
            EntityTargetLivingEntityEvent event = CraftEventFactory.callEntityTargetLivingEvent(this.mob, this.player, EntityTargetEvent.TargetReason.TEMPT);
            if (event.isCancelled()) {
                this.arclight$effectiveTarget = null;
                cir.setReturnValue(false);
                return;
            }
            if (event.getTarget() != null) {
                target = ((CraftLivingEntity) event.getTarget()).getHandle();
            }
        }
        this.arclight$effectiveTarget = target;
        this.player = target instanceof Player ? (Player) target : null;
        cir.setReturnValue(target != null);
    }

    @Overwrite
    public boolean canContinueToUse() {
        LivingEntity target = this.arclight$effectiveTarget;
        if (target == null) return false;
        if (this.canScare()) {
            if (this.mob.distanceToSqr((Entity) target) < 36.0D) {
                if (target.distanceToSqr(this.px, this.py, this.pz) > 0.010000000000000002D) return false;
                if (Math.abs((double) target.getXRot() - this.pRotX) > 5.0D || Math.abs((double) target.getYRot() - this.pRotY) > 5.0D) return false;
            } else {
                this.px = target.getX();
                this.py = target.getY();
                this.pz = target.getZ();
            }
            this.pRotX = (double) target.getXRot();
            this.pRotY = (double) target.getYRot();
        }
        // Keep native continuation -> virtual canUse ordering (including Cat's tame gate).
        return this.canUse();
    }

    @Overwrite
    public void start() {
        LivingEntity target = this.arclight$effectiveTarget;
        if (target == null) return;
        this.px = target.getX();
        this.py = target.getY();
        this.pz = target.getZ();
        this.isRunning = true;
    }

    @Overwrite
    public void tick() {
        this.arclight$runTemptTick();
    }

    @Unique
    public final void arclight$runTemptTick() {
        LivingEntity target = this.arclight$effectiveTarget;
        if (target == null) return;
        this.mob.getLookControl().setLookAt(target, (float) (this.mob.getMaxHeadYRot() + 20), (float) this.mob.getMaxHeadXRot());
        if (this.mob.distanceToSqr((Entity) target) < this.stopDistance * this.stopDistance) {
            this.stopNavigation();
        } else if (target instanceof Player) {
            // Keep original virtual Player dispatch for Player subclass behavior.
            this.navigateTowards((Player) target);
        } else if (this instanceof ForNonPathfindersTemptBridge) {
            ((ForNonPathfindersTemptBridge) this).arclight$navigateTowardsLivingForNonPathfinder(target);
        } else {
            ((TemptGoalBridge) (Object) this).arclight$navigateTowardsLiving(target);
        }
    }

    @Unique
    public final void arclight$navigateTowardsLiving(LivingEntity target) {
        this.mob.getNavigation().moveTo((Entity) target, this.speedModifier);
    }

    @Inject(method = "stop", at = @At("TAIL"))
    private void arclight$clearEffectiveTarget(CallbackInfo ci) {
        this.arclight$effectiveTarget = null;
    }
}
