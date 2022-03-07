package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.PrepareRamNearestTarget;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(PrepareRamNearestTarget.class)
public class PrepareRamNearestTargetMixin {

    @Redirect(method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)V",
        at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"))
    private void arclight$targetEvent(Optional<LivingEntity> instance, Consumer<LivingEntity> action, ServerLevel level, PathfinderMob mob) {
        instance.ifPresent(entity -> {
            var event = CraftEventFactory.callEntityTargetLivingEvent(mob, entity,
                (entity instanceof ServerPlayer) ? EntityTargetEvent.TargetReason.CLOSEST_PLAYER : EntityTargetEvent.TargetReason.CLOSEST_ENTITY);
            if (event.isCancelled() || event.getTarget() == null) {
                return;
            }
            var newEntity = ((CraftLivingEntity) event.getTarget()).getHandle();
            action.accept(newEntity);
        });
    }
}
