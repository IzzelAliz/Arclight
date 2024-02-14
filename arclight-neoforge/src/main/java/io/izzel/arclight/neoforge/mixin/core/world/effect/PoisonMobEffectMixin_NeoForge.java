package io.izzel.arclight.neoforge.mixin.core.world.effect;

import io.izzel.arclight.common.bridge.core.util.DamageSourceBridge;
import io.izzel.arclight.common.bridge.core.util.DamageSourcesBridge;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.effect.PoisonMobEffect")
public class PoisonMobEffectMixin_NeoForge {

    @Redirect(method = "applyEffectTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;source(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/world/damagesource/DamageSource;"))
    private DamageSource arclight$redirectPoison(DamageSources instance, ResourceKey<DamageType> source) {
        return ((DamageSourceBridge) instance.source(source)).bridge$poison();
    }
}
