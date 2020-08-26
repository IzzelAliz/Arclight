package io.izzel.arclight.common.mixin.core.entity.passive;

import io.izzel.arclight.common.bridge.entity.passive.TurtleEntityBridge;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TurtleEntity.class)
public abstract class TurtleEntityMixin extends AnimalEntityMixin implements TurtleEntityBridge {

    // @formatter:off
    @Accessor("isDigging") public abstract int bridge$getDigging();
    @Invoker("setDigging") public abstract void bridge$setDigging(boolean digging);
    @Accessor("isDigging") public abstract void bridge$setDigging(int i);
    @Invoker("setHasEgg") public abstract void bridge$setHasEgg(boolean b);
    // @formatter:on

    @Inject(method = "onGrowingAdult", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/TurtleEntity;entityDropItem(Lnet/minecraft/util/IItemProvider;I)Lnet/minecraft/entity/item/ItemEntity;"))
    private void arclight$forceDrop(CallbackInfo ci) {
        forceDrops = true;
    }

    @Inject(method = "onGrowingAdult", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/passive/TurtleEntity;entityDropItem(Lnet/minecraft/util/IItemProvider;I)Lnet/minecraft/entity/item/ItemEntity;"))
    private void arclight$forceDropReset(CallbackInfo ci) {
        forceDrops = false;
    }

    @Inject(method = "func_241841_a", at = @At("HEAD"))
    private void arclight$lightning(ServerWorld p_241841_1_, LightningBoltEntity lightningBolt, CallbackInfo ci) {
        CraftEventFactory.entityDamage = lightningBolt;
    }

    @Inject(method = "func_241841_a", at = @At("RETURN"))
    private void arclight$lightningReset(ServerWorld p_241841_1_, LightningBoltEntity lightningBolt, CallbackInfo ci) {
        CraftEventFactory.entityDamage = null;
    }
}
