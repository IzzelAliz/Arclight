package io.izzel.arclight.common.mixin.core.entity.ai.goal;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.BreakDoorGoal;
import net.minecraft.entity.ai.goal.InteractDoorGoal;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BreakDoorGoal.class)
public abstract class BreakDoorGoalMixin extends InteractDoorGoal {

    public BreakDoorGoalMixin(MobEntity entityIn) {
        super(entityIn);
    }

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    public void arclight$breakDoor(CallbackInfo ci) {
        if (CraftEventFactory.callEntityBreakDoorEvent(this.entity, this.doorPosition).isCancelled()) {
            this.startExecuting();
            ci.cancel();
        }
    }
}
