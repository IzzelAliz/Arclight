package io.izzel.arclight.common.mixin.core.world.entity.boss.enderdragon.phases;

import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonSittingFlamingPhase;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonSittingFlamingPhase.class)
public class DragonSittingFlamingPhaseMixin {

    @Shadow @Nullable private AreaEffectCloud flame;

    @Inject(method = "end", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/AreaEffectCloud;discard()V"))
    private void arclight$despawn(CallbackInfo ci) {
        this.flame.bridge().bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }
}
