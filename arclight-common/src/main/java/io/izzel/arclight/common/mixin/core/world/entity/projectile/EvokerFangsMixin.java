package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.core.util.DamageSourceBridge;
import io.izzel.arclight.common.mixin.core.world.entity.EntityMixin;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.projectile.EvokerFangs;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EvokerFangs.class)
public abstract class EvokerFangsMixin extends EntityMixin {

    @Redirect(method = "dealDamageTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;magic()Lnet/minecraft/world/damagesource/DamageSource;"))
    private DamageSource arclight$entityDamage(DamageSources instance) {
        return ((DamageSourceBridge) instance.magic()).bridge$customCausingEntity((EvokerFangs) (Object) this);
    }
}
