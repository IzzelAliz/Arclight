package io.izzel.arclight.common.mixin.core.entity.merchant.villager;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.monster.WitchEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.item.MerchantOffer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.VillagerReplenishTradeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import io.izzel.arclight.common.bridge.item.MerchantOfferBridge;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends AbstractVillagerEntityMixin {

    @Inject(method = "updateAITasks", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/merchant/villager/VillagerEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$reason(CallbackInfo ci) {
        bridge$pushEffectCause(EntityPotionEffectEvent.Cause.VILLAGER_TRADE);
    }

    @Redirect(method = "recalculateSpecialPricesFor", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/item/MerchantOffer;increaseSpecialPrice(I)V"))
    private void arclight$replenish(MerchantOffer merchantOffer, int add) {
        VillagerReplenishTradeEvent event = new VillagerReplenishTradeEvent((Villager) this.getBukkitEntity(), ((MerchantOfferBridge) merchantOffer).bridge$asBukkit(), add);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            merchantOffer.increaseSpecialPrice(event.getBonus());
        }
    }

    @Inject(method = "onStruckByLightning", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$transformWitch(LightningBoltEntity lightningBolt, CallbackInfo ci, WitchEntity witchEntity) {
        if (CraftEventFactory.callEntityTransformEvent((VillagerEntity) (Object) this, witchEntity, EntityTransformEvent.TransformReason.LIGHTNING).isCancelled()) {
            ci.cancel();
        } else {
            ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.LIGHTNING);
        }
    }

    @Inject(method = "trySpawnGolem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$ironGolemReason(CallbackInfoReturnable<IronGolemEntity> cir) {
        ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE);
    }
}
