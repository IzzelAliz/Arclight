package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StopAttackingIfTargetInvalid.class)
public class StopAttackingIfTargetInvalidMixin<E extends Mob> {

    @Inject(method = "clearAttackTarget", cancellable = true, at = @At("HEAD"))
    private void arclight$attackEvent(E mob, CallbackInfo ci) {
        LivingEntity old = mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        EntityTargetEvent event = CraftEventFactory.callEntityTargetLivingEvent(mob, old, (old != null && !old.isAlive()) ? EntityTargetEvent.TargetReason.TARGET_DIED : EntityTargetEvent.TargetReason.FORGOT_TARGET);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
        if (event.getTarget() != null) {
            mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, ((CraftLivingEntity)event.getTarget()).getHandle());
            ci.cancel();
        }
    }
}
