package io.izzel.arclight.common.mixin.core.entity;

import net.minecraft.entity.CreatureEntity;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreatureEntity.class)
public abstract class CreatureEntityMixin extends MobEntityMixin {

    @Inject(method = "updateLeashedState", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/CreatureEntity;clearLeashed(ZZ)V"))
    private void arclight$unleashDistance(CallbackInfo ci) {
        Bukkit.getPluginManager().callEvent(new EntityUnleashEvent(this.getBukkitEntity(), EntityUnleashEvent.UnleashReason.DISTANCE));
    }
}
