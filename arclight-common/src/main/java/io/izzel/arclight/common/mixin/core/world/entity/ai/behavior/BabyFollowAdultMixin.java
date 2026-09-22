package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BabyFollowAdult;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.function.Function;

@Mixin(BabyFollowAdult.class)
public abstract class BabyFollowAdultMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static OneShot<LivingEntity> create(UniformInt followRange, Function<LivingEntity, Float> speedModifier, MemoryModuleType<? extends LivingEntity> nearestVisibleType, boolean targetEye) {
        return BehaviorBuilder.create((behaviorbuilderInstance) -> {
            return behaviorbuilderInstance.group(behaviorbuilderInstance.present(nearestVisibleType), behaviorbuilderInstance.registered(MemoryModuleType.LOOK_TARGET), behaviorbuilderInstance.absent(MemoryModuleType.WALK_TARGET)).apply(behaviorbuilderInstance, (memoryAccessor, lookTarget, walkTarget) -> {
                return (serverLevel, baby, gameTime) -> {
                    if (!baby.isBaby()) {
                        return false;
                    } else {
                        LivingEntity adult = behaviorbuilderInstance.get(memoryAccessor);
                        if (baby.closerThan(adult, (double) (followRange.getMaxValue() + 1)) && !baby.closerThan(adult, (double) followRange.getMinValue())) {
                            // CraftBukkit start
                            EntityTargetLivingEntityEvent event = CraftEventFactory.callEntityTargetLivingEvent(baby, adult, EntityTargetEvent.TargetReason.FOLLOW_LEADER);
                            if (event.isCancelled()) {
                                return false;
                            }
                            if (event.getTarget() == null) {
                                memoryAccessor.erase();
                                return true;
                            }
                            adult = ((CraftLivingEntity) event.getTarget()).getHandle();
                            // CraftBukkit end
                            WalkTarget target = new WalkTarget(new EntityTracker(adult, targetEye, targetEye), speedModifier.apply(baby), followRange.getMinValue() - 1);
                            lookTarget.set(new EntityTracker(adult, true, targetEye));
                            walkTarget.set(target);
                            return true;
                        } else {
                            return false;
                        }
                    }
                };
            });
        });
    }
}
