package io.izzel.arclight.common.mixin.core.entity.passive;

import io.izzel.arclight.common.mixin.core.entity.MobEntityMixin;
import net.minecraft.entity.passive.BatEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BatEntity.class)
public abstract class BatEntityMixin extends MobEntityMixin {

    // @formatter:off
    @Shadow public abstract boolean getIsBatHanging();
    // @formatter:on

    @Inject(method = "updateAITasks", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/BatEntity;setIsBatHanging(Z)V"))
    private void arclight$toggleSleep(CallbackInfo ci) {
        if (!CraftEventFactory.handleBatToggleSleepEvent((BatEntity) (Object) this, !this.getIsBatHanging())) {
            ci.cancel();
        }
    }
}
