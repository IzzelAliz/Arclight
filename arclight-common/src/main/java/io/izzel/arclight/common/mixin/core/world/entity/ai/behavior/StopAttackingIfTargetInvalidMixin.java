package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(StopAttackingIfTargetInvalid.class)
public abstract class StopAttackingIfTargetInvalidMixin {

    // @formatter:off
    @Shadow private static boolean isTiredOfTryingToReachTarget(LivingEntity p_259416_, Optional<Long> p_259377_) { return false; }
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static <E extends Mob> BehaviorControl<E> create(StopAttackingIfTargetInvalid.StopAttackCondition stopAttackingWhen, StopAttackingIfTargetInvalid.TargetErasedCallback<E> onTargetErased, boolean canGrowTiredOfTryingToReachTarget) {
        return BehaviorBuilder.create((p_258801_) -> {
            return p_258801_.group(p_258801_.present(MemoryModuleType.ATTACK_TARGET), p_258801_.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)).apply(p_258801_, (p_258787_, p_258788_) -> {
                return (ServerLevel serverLevel, E mob, long gameTime) -> {
                    LivingEntity livingentity = p_258801_.get(p_258787_);
                    if (mob.canAttack(livingentity) && (!canGrowTiredOfTryingToReachTarget || !isTiredOfTryingToReachTarget(mob, p_258801_.tryGet(p_258788_))) && livingentity.isAlive() && livingentity.level() == mob.level() && !stopAttackingWhen.test(serverLevel, livingentity)) {
                        return true;
                    } else {
                        // CraftBukkit start
                        LivingEntity old = mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
                        EntityTargetEvent event = CraftEventFactory.callEntityTargetLivingEvent(mob, null, (old != null && !old.isAlive()) ? EntityTargetEvent.TargetReason.TARGET_DIED : EntityTargetEvent.TargetReason.FORGOT_TARGET);
                        if (event.isCancelled()) {
                            return false;
                        }
                        if (event.getTarget() != null) {
                            mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, ((CraftLivingEntity) event.getTarget()).getHandle());
                            return true;
                        }
                        // CraftBukkit end
                        onTargetErased.accept(serverLevel, mob, livingentity);
                        p_258787_.erase();
                        return true;
                    }
                };
            });
        });
    }
}
