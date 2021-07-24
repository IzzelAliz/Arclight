package io.izzel.arclight.common.mixin.core.world.entity.ambient;

import io.izzel.arclight.common.mixin.core.world.entity.MobMixin;
import net.minecraft.world.entity.ambient.Bat;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Bat.class)
public abstract class BatMixin extends MobMixin {

    // @formatter:off
    @Shadow public abstract boolean isResting();
    // @formatter:on

    @Inject(method = "customServerAiStep", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ambient/Bat;setResting(Z)V"))
    private void arclight$toggleSleep(CallbackInfo ci) {
        if (!CraftEventFactory.handleBatToggleSleepEvent((Bat) (Object) this, !this.isResting())) {
            ci.cancel();
        }
    }
}
