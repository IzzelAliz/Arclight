package io.izzel.arclight.common.mixin.core.entity.ai.brain.task;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.ai.brain.task.CreateBabyVillagerTask;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CreateBabyVillagerTask.class)
public class CreateBabyVillagerTaskMixin {

    @Redirect(method = "createChild", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/merchant/villager/VillagerEntity;func_241840_a(Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/entity/AgeableEntity;)Lnet/minecraft/entity/merchant/villager/VillagerEntity;"))
    private VillagerEntity arclight$entityBreed(VillagerEntity lona, ServerWorld world, AgeableEntity anonymous) {
        VillagerEntity child = lona.func_241840_a(world, anonymous);
        if (child != null && !CraftEventFactory.callEntityBreedEvent(child, lona, anonymous, null, null, 0).isCancelled()) {
            ((WorldBridge) world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BREEDING);
            return child;
        } else {
            return null;
        }
    }
}
