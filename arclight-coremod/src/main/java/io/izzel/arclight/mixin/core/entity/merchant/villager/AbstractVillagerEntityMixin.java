package io.izzel.arclight.mixin.core.entity.merchant.villager;

import io.izzel.arclight.bridge.entity.merchant.IMerchantBridge;
import io.izzel.arclight.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.mixin.core.entity.CreatureEntityMixin;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.MerchantOffer;
import net.minecraft.item.MerchantOffers;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftMerchant;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftMerchantRecipe;
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
import io.izzel.arclight.bridge.item.MerchantOfferBridge;

@Mixin(AbstractVillagerEntity.class)
public abstract class AbstractVillagerEntityMixin extends CreatureEntityMixin implements IMerchantBridge {

    @Shadow @Final private Inventory villagerInventory;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends AbstractVillagerEntity> type, World worldIn, CallbackInfo ci) {
        ((IInventoryBridge) this.villagerInventory).bridge$setOwner((InventoryHolder) this.getBukkitEntity());
    }

    private CraftMerchant craftMerchant;

    @Override
    public CraftMerchant bridge$getCraftMerchant() {
        return (craftMerchant == null) ? craftMerchant = new CraftMerchant((AbstractVillagerEntity) (Object) this) : craftMerchant;
    }

    @Redirect(method = "addTrades", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/MerchantOffers;add(Ljava/lang/Object;)Z"))
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
