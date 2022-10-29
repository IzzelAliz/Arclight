package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(StartAttacking.class)
public class StartAttackingMixin<E extends Mob> {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static <E extends Mob> void setAttackTarget(E mob, LivingEntity livingEntity) {
        EntityTargetEvent event = CraftEventFactory.callEntityTargetLivingEvent(mob, livingEntity, (livingEntity instanceof ServerPlayer) ? EntityTargetEvent.TargetReason.CLOSEST_PLAYER : EntityTargetEvent.TargetReason.CLOSEST_ENTITY);
        if (event.isCancelled()) {
            return;
        }
        livingEntity = ((event.getTarget() != null) ? ((CraftLivingEntity) event.getTarget()).getHandle() : null);
        var changeTargetEvent = ForgeHooks.onLivingChangeTarget(mob, livingEntity, LivingChangeTargetEvent.LivingTargetType.BEHAVIOR_TARGET);
        if (changeTargetEvent.isCanceled()) {
            return;
        }
        livingEntity = changeTargetEvent.getNewTarget();
        mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, livingEntity);
        mob.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        // noinspection removal
        ForgeHooks.onLivingSetAttackTarget(mob, livingEntity, LivingChangeTargetEvent.LivingTargetType.BEHAVIOR_TARGET); // TODO: Remove in 1.20
    }
}
