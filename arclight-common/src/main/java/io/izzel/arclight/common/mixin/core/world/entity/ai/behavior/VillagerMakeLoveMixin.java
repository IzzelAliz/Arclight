package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.VillagerMakeLove;
import net.minecraft.world.entity.npc.villager.Villager;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(VillagerMakeLove.class)
public class VillagerMakeLoveMixin {

    // Retain both callsite continuations, but commit the parents only after the
    // native child initialization and Bukkit acceptance. Shares are per breed call.
    @WrapOperation(method = "breed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;setAge(I)V", ordinal = 0))
    private void arclight$deferSourceAge(Villager instance, int age, Operation<Void> original,
                                       @Share("sourceAge") LocalRef<Runnable> sourceAge) {
        sourceAge.set(() -> original.call(instance, age));
    }

    @WrapOperation(method = "breed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;setAge(I)V", ordinal = 1))
    private void arclight$deferTargetAge(Villager instance, int age, Operation<Void> original,
                                       @Share("targetAge") LocalRef<Runnable> targetAge) {
        targetAge.set(() -> original.call(instance, age));
    }

    @Inject(method = "breed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;snapTo(DDDFF)V", shift = At.Shift.AFTER), cancellable = true)
    private void arclight$entityBreed(ServerLevel world, Villager source, Villager target,
                                     CallbackInfoReturnable<Optional<Villager>> cir,
                                     @Local(index = 4) Villager child,
                                     @Share("sourceAge") LocalRef<Runnable> sourceAge,
                                     @Share("targetAge") LocalRef<Runnable> targetAge) {
        if (CraftEventFactory.callEntityBreedEvent(child, source, target, null, null, 0).isCancelled()) {
            cir.setReturnValue(Optional.empty());
            return;
        }
        sourceAge.get().run();
        targetAge.get().run();
    }

    @WrapOperation(method = "breed", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$breedSpawnReason(ServerLevel world, Entity child, Operation<Void> original) {
        ((WorldBridge) world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BREEDING);
        original.call(world, child);
    }
}
