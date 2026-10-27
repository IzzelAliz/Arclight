package io.izzel.arclight.common.mixin.core.world.inventory;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.item.trading.MerchantBridge;
import io.izzel.arclight.common.bridge.core.world.item.trading.MerchantOfferBridge;
import io.papermc.paper.event.player.PlayerPurchaseEvent;
import io.papermc.paper.event.player.PlayerTradeEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.bukkit.craftbukkit.inventory.CraftMerchantCustom;
import org.bukkit.craftbukkit.inventory.CraftMerchantRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantResultSlot.class)
public abstract class MerchantResultSlotMixin {

    @Shadow @Final private Merchant merchant;

    @WrapOperation(method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/MerchantResultSlot;checkTakeAchievements(Lnet/minecraft/world/item/ItemStack;)V"), require = 1, allow = 1)
    private void arclight$deferPurchaseAchievements(MerchantResultSlot slot, ItemStack carried, Operation<Void> original,
            @Share("arclightPurchaseAchievements") LocalRef<Runnable> achievements) {
        // Retain the supplied continuation, but run it only after the event accepts.
        achievements.set(() -> original.call(slot, carried));
    }

    @WrapOperation(method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/MerchantContainer;getActiveOffer()Lnet/minecraft/world/item/trading/MerchantOffer;"), require = 1, allow = 1)
    private MerchantOffer arclight$purchaseEvent(MerchantContainer container, Operation<MerchantOffer> original,
            @Local(argsOnly = true) Player player, @Local(argsOnly = true) ItemStack carried,
            @Share("arclightPurchaseEvent") LocalRef<PlayerPurchaseEvent> purchaseEvent,
            @Share("arclightPurchaseAchievements") LocalRef<Runnable> achievements,
            @Cancellable CallbackInfo ci) {
        MerchantOffer selectedOffer = original.call(container);
        PlayerPurchaseEvent event = null;
        if (selectedOffer != null && player instanceof ServerPlayer serverPlayer) {
            var bukkitPlayer = ((ServerPlayerBridge) serverPlayer).bridge$getBukkitEntity();
            if (this.merchant instanceof AbstractVillager villager) {
                // Both events share PlayerPurchaseEvent's HandlerList: dispatch only the subtype.
                event = new PlayerTradeEvent(bukkitPlayer,
                    (org.bukkit.entity.AbstractVillager) ((EntityBridge) villager).bridge$getBukkitEntity(),
                    ((MerchantOfferBridge) selectedOffer).bridge$asBukkit(), true, true);
            } else if (this.merchant instanceof CraftMerchantCustom.MinecraftMerchant customMerchant) {
                event = new PlayerPurchaseEvent(bukkitPlayer, customMerchant.getCraftMerchant(),
                    ((MerchantOfferBridge) selectedOffer).bridge$asBukkit(), false, true);
            }
            if (event != null) {
                if (!event.callEvent()) {
                    carried.setCount(0);
                    player.containerMenu.sendAllDataToRemote();
                    int level = this.merchant instanceof Villager villager ? villager.getVillagerData().level() : 1;
                    serverPlayer.sendMerchantOffers(player.containerMenu.containerId, this.merchant.getOffers(), level,
                        this.merchant.getVillagerXp(), this.merchant.showProgressBar(), this.merchant.canRestock());
                    ci.cancel();
                    return null;
                }
                selectedOffer = CraftMerchantRecipe.fromBukkit(event.getTrade()).toMinecraft();
            }
        }
        purchaseEvent.set(event);
        achievements.get().run();
        // Both native take orientations and the XP tail consume this same settled offer.
        return selectedOffer;
    }

    @WrapOperation(method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/Merchant;notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V"), require = 1, allow = 1)
    private void arclight$processPurchase(Merchant merchant, MerchantOffer offer, Operation<Void> original,
            @Share("arclightPurchaseEvent") LocalRef<PlayerPurchaseEvent> purchaseEvent) {
        PlayerPurchaseEvent event = purchaseEvent.get();
        if (merchant instanceof CraftMerchantCustom.MinecraftMerchant customMerchant) {
            // Imported Craft already implements Paper's custom-merchant uses/XP policy.
            customMerchant.processTrade(offer, event);
            return;
        }
        if (!(merchant instanceof AbstractVillager)) {
            // Unrelated mod merchants keep their native notification, without a bridge cast.
            original.call(merchant, offer);
            return;
        }
        MerchantBridge bridge = (MerchantBridge) merchant;
        PlayerPurchaseEvent previous = bridge.bridge$exchangePurchaseEvent(event);
        try {
            original.call(merchant, offer);
        } finally {
            bridge.bridge$exchangePurchaseEvent(previous);
        }
    }
}
