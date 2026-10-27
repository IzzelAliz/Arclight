package io.izzel.arclight.common.mixin.core.world.entity;

import com.destroystokyo.paper.event.entity.EntityJumpEvent;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpEventMixin {

    @Decorate(method = "aiStep()V", inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$initJumpDecision(@Local(allocate = "arclightAiJumpAllowed") boolean allowed) throws Throwable {
        allowed = true;
        DecorationOps.blackhole().invoke(allowed);
    }

    // Paper dispatches at the AI caller, not inside jumpFromGround(). In particular,
    // vetoing here also skips a Rabbit override's extra movement and animation tail.
    @Decorate(method = "aiStep()V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;jumpFromGround()V"), require = 1, allow = 1)
    private void arclight$entityJump(LivingEntity entity,
                                   @Local(allocate = "arclightAiJumpAllowed") boolean allowed) throws Throwable {
        allowed = entity.level().isClientSide()
            || new EntityJumpEvent(((LivingEntityBridge) entity).bridge$getBukkitEntity()).callEvent();
        DecorationOps.blackhole().invoke(allowed);
        if (allowed) {
            // Retain the virtual native/mod continuation, including NeoForge's jump hook.
            DecorationOps.callsite().invoke(entity);
        } else {
            entity.setJumping(false);
        }
    }

    // Both native inputs write noJumpDelay in this order: decrement, accepted jump,
    // non-jumping reset. Only the middle write belongs to Paper's event decision.
    @Decorate(method = "aiStep()V", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, ordinal = 1,
        target = "Lnet/minecraft/world/entity/LivingEntity;noJumpDelay:I"), require = 1, allow = 1)
    private void arclight$jumpDelay(LivingEntity entity, int delay,
                                   @Local(allocate = "arclightAiJumpAllowed") boolean allowed) throws Throwable {
        if (allowed) {
            DecorationOps.callsite().invoke(entity, delay);
        }
    }
}
