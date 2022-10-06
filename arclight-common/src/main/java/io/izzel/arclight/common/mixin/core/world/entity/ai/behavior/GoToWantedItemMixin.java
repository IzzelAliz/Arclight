package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.GoToWantedItem;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(GoToWantedItem.class)
public abstract class GoToWantedItemMixin<E extends LivingEntity> {

    // @formatter:off
    @Shadow protected abstract ItemEntity getClosestLovedItem(E p_23156_);
    @Shadow @Final private float speedModifier;
    // @formatter:on

    @Inject(method = "start", cancellable = true, at = @At("HEAD"))
    private void arclight$entityTarget(ServerLevel level, E entity, long p_23154_, CallbackInfo ci) {
        if (entity instanceof Allay) {
            Entity target = this.getClosestLovedItem(entity);
            var event = CraftEventFactory.callEntityTargetEvent(entity, target, EntityTargetEvent.TargetReason.CLOSEST_ENTITY);

            if (event.isCancelled()) {
                ci.cancel();
            }

            target = (event.getTarget() == null) ? null : ((CraftEntity) event.getTarget()).getHandle();
            if (target instanceof ItemEntity item) {
                entity.getBrain().setMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, Optional.of(item));
                BehaviorUtils.setWalkAndLookTargetMemories(entity, target, this.speedModifier, 0);
            } else {
                entity.getBrain().eraseMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
            }
            ci.cancel();
        }
    }
}
