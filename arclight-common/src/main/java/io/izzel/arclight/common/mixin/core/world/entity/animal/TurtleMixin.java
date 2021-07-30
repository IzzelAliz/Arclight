package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.entity.passive.TurtleEntityBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.Turtle;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

    @Inject(method = "thunderHit", at = @At("HEAD"))
    private void arclight$lightning(ServerLevel world, LightningBolt lightningBolt, CallbackInfo ci) {
        CraftEventFactory.entityDamage = lightningBolt;
    }

    @Inject(method = "thunderHit", at = @At("RETURN"))
    private void arclight$lightningReset(ServerLevel world, LightningBolt lightningBolt, CallbackInfo ci) {
        CraftEventFactory.entityDamage = null;
    }
}
