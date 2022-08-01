package io.izzel.arclight.common.mixin.core.world.entity.npc;

import io.izzel.arclight.common.bridge.core.item.MerchantOfferBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.trading.MerchantOffer;
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
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(net.minecraft.world.entity.npc.Villager.class)
public abstract class VillagerMixin extends AbstractVillagerMixin {

    @Inject(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/Villager;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private void arclight$reason(CallbackInfo ci) {
        bridge$pushEffectCause(EntityPotionEffectEvent.Cause.VILLAGER_TRADE);
    }

    @Redirect(method = "restock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;resetUses()V"))
    private void arclight$restock(MerchantOffer instance) {
        VillagerReplenishTradeEvent event = new VillagerReplenishTradeEvent((Villager) this.getBukkitEntity(), ((MerchantOfferBridge) instance).bridge$asBukkit());
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            instance.resetUses();
        }
    }

    @Redirect(method = "catchUpDemand", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;resetUses()V"))
    private void arclight$replenish(MerchantOffer instance) {
        VillagerReplenishTradeEvent event = new VillagerReplenishTradeEvent((Villager) this.getBukkitEntity(), ((MerchantOfferBridge) instance).bridge$asBukkit());
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            instance.resetUses();
        }
    }

    @Inject(method = "thunderHit", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$transformWitch(ServerLevel serverWorld, LightningBolt lightningBolt, CallbackInfo ci, Witch witchEntity) {
        if (CraftEventFactory.callEntityTransformEvent((net.minecraft.world.entity.npc.Villager) (Object) this, witchEntity, EntityTransformEvent.TransformReason.LIGHTNING).isCancelled()) {
            ci.cancel();
        } else {
            ((WorldBridge) serverWorld).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.LIGHTNING);
        }
    }

    @Inject(method = "spawnGolemIfNeeded", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/SpawnUtil;trySpawnMob(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;IIILnet/minecraft/util/SpawnUtil$Strategy;)Ljava/util/Optional;"))
    private void arclight$ironGolemReason(ServerLevel world, long p_35399_, int p_35400_, CallbackInfo ci) {
        ((WorldBridge) world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE);
    }

    @Inject(method = "spawnGolemIfNeeded", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/util/SpawnUtil;trySpawnMob(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;IIILnet/minecraft/util/SpawnUtil$Strategy;)Ljava/util/Optional;"))
    private void arclight$ironGolemReasonReset(ServerLevel world, long p_35399_, int p_35400_, CallbackInfo ci) {
        ((WorldBridge) world).bridge$pushAddEntityReason(null);
    }
}
