package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BabyFollowAdult;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Function;

@Mixin(BabyFollowAdult.class)
public abstract class BabyFollowAdultMixin<E extends AgeableMob> {

    // @formatter:off
    @Shadow protected abstract AgeableMob getNearestAdult(E p_147430_);
    @Shadow @Final private Function<LivingEntity, Float> speedModifier;
    @Shadow @Final private UniformInt followRange;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void start(ServerLevel p_147426_, E entity, long p_147428_) {
        var event = CraftEventFactory.callEntityTargetLivingEvent(entity, this.getNearestAdult(entity), EntityTargetEvent.TargetReason.FOLLOW_LEADER);
        if (event.isCancelled()) {
            return;
        }
        if (event.getTarget() != null) {
            BehaviorUtils.setWalkAndLookTargetMemories(entity, ((CraftLivingEntity) event.getTarget()).getHandle(), this.speedModifier.apply(entity), this.followRange.getMinValue() - 1);
        } else {
            entity.getBrain().eraseMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT);
        }
    }
}
