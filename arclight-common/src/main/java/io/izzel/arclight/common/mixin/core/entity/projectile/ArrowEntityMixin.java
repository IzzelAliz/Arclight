package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.izzel.arclight.common.bridge.entity.projectile.ArrowEntityBridge;

import java.util.Set;

@Mixin(ArrowEntity.class)
@Implements(@Interface(iface = ArrowEntityBridge.Hack.class, prefix = "hack$"))
public abstract class ArrowEntityMixin extends AbstractArrowEntityMixin implements ArrowEntityBridge {

    // @formatter:off
    @Shadow @Final private static DataParameter<Integer> COLOR;
    @Shadow @Final public Set<EffectInstance> customPotionEffects;
    @Shadow private Potion potion;
    // @formatter:on

    @Inject(method = "arrowHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$arrowHit(LivingEntity living, CallbackInfo ci) {
        ((LivingEntityBridge) living).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ARROW);
    }

    public void refreshEffects() {
        this.getDataManager().set(COLOR, PotionUtils.getPotionColorFromEffectList(PotionUtils.mergeEffects(this.potion, this.customPotionEffects)));
    }

    @Override
    public void bridge$refreshEffects() {
        refreshEffects();
    }

    public String hack$getType() {
        return Registry.POTION.getKey(this.potion).toString();
    }

    public void hack$setType(final String string) {
        this.potion = Registry.POTION.getOrDefault(new ResourceLocation(string));
        this.getDataManager().set(COLOR, PotionUtils.getPotionColorFromEffectList(PotionUtils.mergeEffects(this.potion, this.customPotionEffects)));
    }

    public boolean isTipped() {
        return !this.customPotionEffects.isEmpty() || this.potion != Potions.EMPTY;
    }

    @Override
    public boolean bridge$isTipped() {
        return isTipped();
    }
}
