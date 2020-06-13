package io.izzel.arclight.common.mixin.core.entity.ai.goal;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.entity.passive.AnimalEntityBridge;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.ai.goal.BreedGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityBreedEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Random;

@Mixin(BreedGoal.class)
public class BreedGoalMixin {

    // @formatter:off
    @Shadow @Final protected AnimalEntity animal;
    @Shadow protected AnimalEntity targetMate;
    @Shadow @Final protected World world;
    // @formatter:on

    private transient int arclight$exp;

    @Inject(method = "spawnBaby", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/entity/passive/AnimalEntity;getLoveCause()Lnet/minecraft/entity/player/ServerPlayerEntity;"))
    public void arclight$persist(CallbackInfo ci, AgeableEntity ageableEntity) {
        if (ageableEntity instanceof TameableEntity && ((TameableEntity) ageableEntity).isTamed()) {
            ((LivingEntityBridge) ageableEntity).bridge$setPersist(true);
        }
        ServerPlayerEntity playerEntity = this.animal.getLoveCause();
        if (playerEntity == null && this.targetMate.getLoveCause() != null) {
            playerEntity = this.targetMate.getLoveCause();
        }
        arclight$exp = this.animal.getRNG().nextInt(7) + 1;
        EntityBreedEvent event = CraftEventFactory.callEntityBreedEvent(ageableEntity, this.animal, this.targetMate, playerEntity, ((AnimalEntityBridge) this.animal).bridge$getBreedItem(), arclight$exp);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
        arclight$exp = event.getExperience();
    }

    @Inject(method = "spawnBaby", cancellable = true, at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Random;nextInt(I)I"))
    public void arclight$returnIfFail(CallbackInfo ci) {
        if (arclight$exp <= 0) {
            ci.cancel();
        }
    }

    @Redirect(method = "spawnBaby", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Random;nextInt(I)I"))
    public int arclight$setExp(Random random, int bound) {
        return arclight$exp - 1;
    }

}
