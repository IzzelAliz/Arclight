package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.AgeableMob;
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
    public static OneShot<AgeableMob> create(UniformInt p_259321_, Function<LivingEntity, Float> p_259190_) {
        return BehaviorBuilder.create((p_258331_) -> {
            return p_258331_.group(p_258331_.present(MemoryModuleType.NEAREST_VISIBLE_ADULT), p_258331_.registered(MemoryModuleType.LOOK_TARGET), p_258331_.absent(MemoryModuleType.WALK_TARGET)).apply(p_258331_, (p_258317_, p_258318_, p_258319_) -> {
                return (p_258326_, p_258327_, p_258328_) -> {
                    if (!p_258327_.isBaby()) {
                        return false;
                    } else {
                        LivingEntity ageablemob = p_258331_.get(p_258317_);
                        if (p_258327_.closerThan(ageablemob, (double) (p_259321_.getMaxValue() + 1)) && !p_258327_.closerThan(ageablemob, (double) p_259321_.getMinValue())) {
                            // CraftBukkit start
                            EntityTargetLivingEntityEvent event = CraftEventFactory.callEntityTargetLivingEvent(p_258327_, ageablemob, EntityTargetEvent.TargetReason.FOLLOW_LEADER);
                            if (event.isCancelled()) {
                                return false;
                            }
                            if (event.getTarget() == null) {
                                p_258317_.erase();
                                return true;
                            }
                            ageablemob = ((CraftLivingEntity) event.getTarget()).getHandle();
                            // CraftBukkit end
                            WalkTarget walktarget = new WalkTarget(new EntityTracker(ageablemob, false), p_259190_.apply(p_258327_), p_259321_.getMinValue() - 1);
                            p_258318_.set(new EntityTracker(ageablemob, true));
                            p_258319_.set(walktarget);
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
