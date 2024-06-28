package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.K1;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StartAttacking.class)
public class StartAttackingMixin {

    @SuppressWarnings({"unchecked", "MixinAnnotationTarget"})
    @Decorate(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;set(Ljava/lang/Object;)V"))
    private static <F extends K1, Value> void arclight$targetEvent(MemoryAccessor<F, Value> instance, Value object, @Local(ordinal = -1) Mob mob) throws Throwable {
        var newTarget = (LivingEntity) object;
        EntityTargetEvent event = CraftEventFactory.callEntityTargetLivingEvent(mob, newTarget, (newTarget instanceof ServerPlayer) ? EntityTargetEvent.TargetReason.CLOSEST_PLAYER : EntityTargetEvent.TargetReason.CLOSEST_ENTITY);
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke(false);
            return;
        }
        if (event.getTarget() == null) {
            instance.erase();
            DecorationOps.cancel().invoke(false);
            return;
        }
        object = (Value) ((CraftLivingEntity) event.getTarget()).getHandle();
        DecorationOps.callsite().invoke(instance, object);
    }
}
