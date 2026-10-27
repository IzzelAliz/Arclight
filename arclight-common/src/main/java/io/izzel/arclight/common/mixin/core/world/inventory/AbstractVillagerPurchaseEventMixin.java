package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.item.trading.MerchantBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.player.PlayerPurchaseEvent;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerPurchaseEventMixin implements MerchantBridge {

    private PlayerPurchaseEvent arclight$pendingPurchaseEvent;

    @Override
    public PlayerPurchaseEvent bridge$exchangePurchaseEvent(PlayerPurchaseEvent event) {
        PlayerPurchaseEvent previous = this.arclight$pendingPurchaseEvent;
        this.arclight$pendingPurchaseEvent = event;
        return previous;
    }

    @Decorate(method = "notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V", inject = true,
        at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$capturePurchaseDecision(
            @Local(allocate = "arclightPurchaseDecision") PlayerPurchaseEvent event) throws Throwable {
        event = this.arclight$pendingPurchaseEvent;
        this.arclight$pendingPurchaseEvent = null;
        DecorationOps.blackhole().invoke(event);
    }

    @Decorate(method = "notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;increaseUses()V"), require = 1, allow = 1)
    private void arclight$purchaseUses(MerchantOffer offer,
            @Local(allocate = "arclightPurchaseDecision") PlayerPurchaseEvent event) throws Throwable {
        if (event == null || event.willIncreaseTradeUses()) {
            DecorationOps.callsite().invoke(offer);
        }
    }

    @Decorate(method = "notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/AbstractVillager;rewardTradeXp(Lnet/minecraft/world/item/trading/MerchantOffer;)V"), require = 1, allow = 1)
    private void arclight$purchaseExperience(AbstractVillager villager, MerchantOffer offer,
            @Local(allocate = "arclightPurchaseDecision") PlayerPurchaseEvent event) throws Throwable {
        if (event == null || event.isRewardingExp()) {
            DecorationOps.callsite().invoke(villager, offer);
        }
        // Keep the ambient/advancement tail and NeoForge's TradeWithVillagerEvent untouched.
    }
}
