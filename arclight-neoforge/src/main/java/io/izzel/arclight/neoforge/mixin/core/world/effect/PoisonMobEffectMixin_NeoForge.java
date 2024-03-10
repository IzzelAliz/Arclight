package io.izzel.arclight.neoforge.mixin.core.world.effect;

import io.izzel.arclight.common.bridge.core.util.DamageSourceBridge;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.minecraft.world.effect.PoisonMobEffect")
public class PoisonMobEffectMixin_NeoForge {

    @ModifyArg(method = "applyEffectTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private DamageSource arclight$redirectPoison(DamageSource source) {
        return ((DamageSourceBridge) source).bridge$poison();
    }
}
