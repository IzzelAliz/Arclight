package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.projectile.ArrowEntityBridge;
import net.minecraft.core.Registry;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(Arrow.class)
public abstract class ArrowEntityMixin extends AbstractArrowMixin implements ArrowEntityBridge {

    // @formatter:off
    @Shadow @Final private static EntityDataAccessor<Integer> ID_EFFECT_COLOR;
    @Shadow @Final public Set<MobEffectInstance> effects;
    @Shadow private Potion potion;
    // @formatter:on

    @Inject(method = "doPostHurtEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$arrowHit(LivingEntity living, CallbackInfo ci) {
        ((LivingEntityBridge) living).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ARROW);
    }

    public void refreshEffects() {
        this.getEntityData().set(ID_EFFECT_COLOR, PotionUtils.getColor(PotionUtils.getAllEffects(this.potion, this.effects)));
    }

    @Override
    public void bridge$refreshEffects() {
        refreshEffects();
    }

    public String getPotionType() {
        return Registry.POTION.getKey(this.potion).toString();
    }

    public void setPotionType(final String string) {
        this.potion = Registry.POTION.get(new ResourceLocation(string));
        this.getEntityData().set(ID_EFFECT_COLOR, PotionUtils.getColor(PotionUtils.getAllEffects(this.potion, this.effects)));
    }

    public boolean isTipped() {
        return !this.effects.isEmpty() || this.potion != Potions.EMPTY;
    }

    @Override
    public boolean bridge$isTipped() {
        return isTipped();
    }
}
