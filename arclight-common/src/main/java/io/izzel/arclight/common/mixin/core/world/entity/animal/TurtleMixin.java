package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.entity.passive.TurtleEntityBridge;
import io.izzel.arclight.common.bridge.core.util.DamageSourceBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.Turtle;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Turtle.class)
public abstract class TurtleMixin extends AnimalMixin implements TurtleEntityBridge {

    // @formatter:off
    @Accessor("layEggCounter") public abstract int bridge$getDigging();
    @Invoker("setLayingEgg") public abstract void bridge$setDigging(boolean digging);
    @Accessor("layEggCounter") public abstract void bridge$setDigging(int i);
    @Invoker("setHasEgg") public abstract void bridge$setHasEgg(boolean b);
    // @formatter:on

    @Inject(method = "ageBoundaryReached", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Turtle;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;I)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private void arclight$forceDrop(CallbackInfo ci) {
        forceDrops = true;
    }

    @Inject(method = "ageBoundaryReached", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/animal/Turtle;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;I)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private void arclight$forceDropReset(CallbackInfo ci) {
        forceDrops = false;
    }

    @Redirect(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;lightningBolt()Lnet/minecraft/world/damagesource/DamageSource;"))
    private DamageSource arclight$lightning(DamageSources instance, ServerLevel serverLevel, LightningBolt lightningBolt) {
        return ((DamageSourceBridge) instance.lightningBolt()).bridge$customCausingEntity(lightningBolt);
    }
}
