package io.izzel.arclight.common.mixin.core.world.entity.npc;

import io.izzel.arclight.common.bridge.core.entity.merchant.IMerchantBridge;
import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.item.MerchantOfferBridge;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.inventory.CraftMerchant;
import org.bukkit.craftbukkit.v.inventory.CraftMerchantRecipe;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.entity.npc.AbstractVillager.class)
public abstract class AbstractVillagerMixin extends PathfinderMobMixin implements IMerchantBridge {

    @Shadow @Final private SimpleContainer inventory;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends net.minecraft.world.entity.npc.AbstractVillager> type, Level worldIn, CallbackInfo ci) {
        ((IInventoryBridge) this.inventory).setOwner((InventoryHolder) this.getBukkitEntity());
    }

    private CraftMerchant craftMerchant;

    @Override
    public CraftMerchant bridge$getCraftMerchant() {
        return (craftMerchant == null) ? craftMerchant = new CraftMerchant((net.minecraft.world.entity.npc.AbstractVillager) (Object) this) : craftMerchant;
    }

    @Redirect(method = "addOffersFromItemListings", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/item/trading/MerchantOffers;add(Ljava/lang/Object;)Z"))
    private boolean arclight$gainOffer(MerchantOffers merchantOffers, Object e) {
        MerchantOffer offer = (MerchantOffer) e;
        VillagerAcquireTradeEvent event = new VillagerAcquireTradeEvent((AbstractVillager) getBukkitEntity(), ((MerchantOfferBridge) offer).bridge$asBukkit());
        if (this.valid) {
            Bukkit.getPluginManager().callEvent(event);
        }
        if (!event.isCancelled()) {
            return merchantOffers.add(CraftMerchantRecipe.fromBukkit(event.getRecipe()).toMinecraft());
        }
        return false;
    }
}
