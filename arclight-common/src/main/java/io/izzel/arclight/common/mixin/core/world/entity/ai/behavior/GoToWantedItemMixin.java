package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.K1;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.GoToWantedItem;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.function.Predicate;

@Mixin(GoToWantedItem.class)
public abstract class GoToWantedItemMixin<E extends LivingEntity> {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static <E extends LivingEntity> BehaviorControl<E> create(Predicate<E> p_259490_, float p_260346_, boolean p_259637_, int p_259054_) {
        return BehaviorBuilder.create((p_258371_) -> {
            BehaviorBuilder<E, ? extends MemoryAccessor<? extends K1, WalkTarget>> behaviorbuilder = p_259637_ ? p_258371_.registered(MemoryModuleType.WALK_TARGET) : p_258371_.absent(MemoryModuleType.WALK_TARGET);
            return p_258371_.group(p_258371_.registered(MemoryModuleType.LOOK_TARGET), behaviorbuilder, p_258371_.present(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM), p_258371_.registered(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS)).apply(p_258371_, (p_258387_, p_258388_, p_258389_, p_258390_) -> {
                return (p_258380_, p_258381_, p_258382_) -> {
                    ItemEntity itementity = p_258371_.get(p_258389_);
                    if (p_258371_.tryGet(p_258390_).isEmpty() && p_259490_.test(p_258381_) && itementity.closerThan(p_258381_, (double) p_259054_) && p_258381_.level().getWorldBorder().isWithinBounds(itementity.blockPosition())) {
                        // CraftBukkit start
                        if (p_258381_ instanceof net.minecraft.world.entity.animal.allay.Allay) {
                            var event = CraftEventFactory.callEntityTargetEvent(p_258381_, itementity, EntityTargetEvent.TargetReason.CLOSEST_ENTITY);

                            if (event.isCancelled()) {
                                return false;
                            }
                            if (!(event.getTarget() instanceof ItemEntity)) {
                                p_258389_.erase();
                            }

                            itementity = (ItemEntity) ((CraftEntity) event.getTarget()).getHandle();
                        }
                        // CraftBukkit end

                        WalkTarget walktarget = new WalkTarget(new EntityTracker(itementity, false), p_260346_, 0);
                        p_258387_.set(new EntityTracker(itementity, true));
                        p_258388_.set(walktarget);
                        return true;
                    } else {
                        return false;
                    }
                };
            });
        });
    }
}
