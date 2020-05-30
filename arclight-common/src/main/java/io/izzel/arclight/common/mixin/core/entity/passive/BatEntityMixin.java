package io.izzel.arclight.common.mixin.core.entity.passive;

import io.izzel.arclight.common.mixin.core.entity.MobEntityMixin;
import net.minecraft.entity.passive.BatEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BatEntity.class)
public abstract class BatEntityMixin extends MobEntityMixin {

    private transient boolean arclight$muteFirst;

    @Inject(method = "setIsBatHanging", cancellable = true, at = @At("HEAD"))
    public void arclight$toggleSleep(boolean isHanging, CallbackInfo ci) {
        if (!arclight$muteFirst) {
            arclight$muteFirst = true;
            return;
        }
        if (!CraftEventFactory.handleBatToggleSleepEvent((BatEntity) (Object) this, !isHanging)) {
            ci.cancel();
        }
    }
}
