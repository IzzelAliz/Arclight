package io.izzel.arclight.common.mixin.optimization.general.realtime;

import io.izzel.arclight.common.mod.ArclightConstants;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin_Realtime {

    @Shadow public int pickupDelay;
    @Shadow public int age;

    private int lastTick = ArclightConstants.currentTick - 1;

    @Inject(method = "tick", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void arclight$useWallTime(CallbackInfo ci) {
        int elapsedTicks = ArclightConstants.currentTick - this.lastTick - 1;
        if (elapsedTicks < 0) {
            elapsedTicks = 0;
        }
        if (this.pickupDelay > 0 && this.pickupDelay != 32767 && elapsedTicks > 0) this.pickupDelay -= elapsedTicks;
        if (this.age != -32768) this.age += elapsedTicks;
        this.lastTick = ArclightConstants.currentTick;
    }
}
