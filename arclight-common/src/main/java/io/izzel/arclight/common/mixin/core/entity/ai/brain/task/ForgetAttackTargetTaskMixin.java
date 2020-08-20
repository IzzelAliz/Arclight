package io.izzel.arclight.common.mixin.core.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.ForgetAttackTargetTask;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ForgetAttackTargetTask.class)
public class ForgetAttackTargetTaskMixin<E extends MobEntity> {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void func_233976_a_(E mob, LivingEntity livingEntity) {
        EntityTargetEvent event = CraftEventFactory.callEntityTargetLivingEvent(mob, livingEntity, (livingEntity instanceof ServerPlayerEntity) ? EntityTargetEvent.TargetReason.CLOSEST_PLAYER : EntityTargetEvent.TargetReason.CLOSEST_ENTITY);
        if (event.isCancelled()) {
            return;
        }
        livingEntity = ((event.getTarget() != null) ? ((CraftLivingEntity) event.getTarget()).getHandle() : null);
        mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, livingEntity);
        mob.getBrain().removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }
}
