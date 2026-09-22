package io.izzel.arclight.common.mixin.core.world.entity.animal;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.izzel.arclight.common.bridge.core.world.entity.BeeBridge;
import io.izzel.arclight.common.bridge.core.world.entity.BeePollinateGoalBridge;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.bee.Bee;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Bee.class)
public abstract class BeeMixin extends AnimalMixin implements BeeBridge {

    @Shadow private int stayOutOfHiveCountdown;

    @Invoker("setHasNectar")
    @Override public abstract void bridge$setHasNectar(boolean nectar);

    @Invoker("setHasStung")
    @Override public abstract void bridge$setHasStung(boolean stung);

    @Override
    public int bridge$getCannotEnterHiveTicks() {
        return this.stayOutOfHiveCountdown;
    }

    @WrapOperation(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/bee/Bee$BeePollinateGoal;stopPollinating()V"))
    private void arclight$deferNativePollinationStop(@Coerce Object goal, Operation<Void> original, @Share("arclightPollinateGoal") LocalRef<BeePollinateGoalBridge> deferred) {
        deferred.set((BeePollinateGoalBridge) goal);
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void arclight$stopPollinatingOnlyAfterSuccessfulDamage(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir, @Share("arclightPollinateGoal") LocalRef<BeePollinateGoalBridge> deferred) {
        if (cir.getReturnValueZ() && deferred.get() != null) {
            deferred.get().bridge$stopPollinating();
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void arclight$removePos(ValueOutput output, CallbackInfo ci) {
        if (this.arclight$saveNotIncludeAll) {
            output.discard("hive_pos");
            output.discard("flower_pos");
        }
    }

    @Override
    protected void arclight$postMergeSelectiveSave(CompoundTag tag, boolean includeAll) {
        super.arclight$postMergeSelectiveSave(tag, includeAll);
        if (!includeAll) {
            tag.remove("hive_pos");
            tag.remove("flower_pos");
        }
    }

    @Inject(method = "doHurtTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$sting(ServerLevel serverLevel, Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        ((LivingEntityBridge) entityIn).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
    }
}
