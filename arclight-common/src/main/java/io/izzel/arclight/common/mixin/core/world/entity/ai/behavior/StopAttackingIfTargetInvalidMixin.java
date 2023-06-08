package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

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
    public static <E extends Mob> BehaviorControl<E> create(Predicate<LivingEntity> p_260357_, BiConsumer<E, LivingEntity> p_259568_, boolean p_260319_) {
        return BehaviorBuilder.create((p_258801_) -> {
            return p_258801_.group(p_258801_.present(MemoryModuleType.ATTACK_TARGET), p_258801_.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)).apply(p_258801_, (p_258787_, p_258788_) -> {
                return (p_258795_, p_258796_, p_258797_) -> {
                    LivingEntity livingentity = p_258801_.get(p_258787_);
                    if (p_258796_.canAttack(livingentity) && (!p_260319_ || !isTiredOfTryingToReachTarget(p_258796_, p_258801_.tryGet(p_258788_))) && livingentity.isAlive() && livingentity.level() == p_258796_.level() && !p_260357_.test(livingentity)) {
                        return true;
                    } else {
                        // CraftBukkit start
                        LivingEntity old = p_258796_.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
                        EntityTargetEvent event = CraftEventFactory.callEntityTargetLivingEvent(p_258796_, null, (old != null && !old.isAlive()) ? EntityTargetEvent.TargetReason.TARGET_DIED : EntityTargetEvent.TargetReason.FORGOT_TARGET);
                        if (event.isCancelled()) {
                            return false;
                        }
                        if (event.getTarget() == null) {
                            p_258787_.erase();
                            return true;
                        }
                        livingentity = ((CraftLivingEntity) event.getTarget()).getHandle();
                        // CraftBukkit end
                        p_259568_.accept(p_258796_, livingentity);
                        p_258787_.erase();
                        return true;
                    }
                };
            });
        });
    }
}
