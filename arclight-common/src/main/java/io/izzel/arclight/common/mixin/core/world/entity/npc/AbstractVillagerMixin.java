package io.izzel.arclight.common.mixin.core.world.entity.npc;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.item.trading.MerchantBridge;
import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.world.item.trading.MerchantOfferBridge;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import net.minecraft.core.HolderSet;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftAbstractVillager;
import org.bukkit.craftbukkit.inventory.CraftMerchant;
import org.bukkit.craftbukkit.inventory.CraftMerchantRecipe;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.entity.npc.villager.AbstractVillager.class)
public abstract class AbstractVillagerMixin extends PathfinderMobMixin implements MerchantBridge {

    @Shadow @Final private SimpleContainer inventory;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends net.minecraft.world.entity.npc.villager.AbstractVillager> type, Level worldIn, CallbackInfo ci) {
        ((IInventoryBridge) this.inventory).setOwner((InventoryHolder) this.getBukkitEntity());
    }

    private CraftMerchant craftMerchant;

    @Override
    public CraftMerchant bridge$getCraftMerchant() {
        return (craftMerchant == null) ? craftMerchant = new CraftAbstractVillager(((CraftServer) Bukkit.getServer()), (net.minecraft.world.entity.npc.villager.AbstractVillager) (Object) this) : craftMerchant;
    }

    // Both native helpers are static. The TradeSet context carries the owning villager;
    // fixed CraftBukkit dispatches acquisition only in the without-duplicates branch.
    @WrapOperation(method = "addOffersFromItemListingsWithoutDuplicates(Lnet/minecraft/world/level/storage/loot/LootContext;Lnet/minecraft/world/item/trading/MerchantOffers;Lnet/minecraft/core/HolderSet;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffers;add(Ljava/lang/Object;)Z"), require = 1, expect = 1, allow = 1)
    private static boolean arclight$gainOffer(MerchantOffers merchantOffers, Object e, Operation<Boolean> original,
            LootContext lootContext, MerchantOffers offers, HolderSet<VillagerTrade> potentialOffers, int numberOfOffers) {
        MerchantOffer offer = (MerchantOffer) e;
        EntityBridge villager = (EntityBridge) lootContext.getParameter(LootContextParams.THIS_ENTITY);
        VillagerAcquireTradeEvent event = new VillagerAcquireTradeEvent((AbstractVillager) villager.bridge$getBukkitEntity(), ((MerchantOfferBridge) offer).bridge$asBukkit());
        if (villager.bridge$isValid()) {
            Bukkit.getPluginManager().callEvent(event);
        }
        if (!event.isCancelled()) {
            return original.call(merchantOffers, CraftMerchantRecipe.fromBukkit(event.getRecipe()).toMinecraft());
        }
        // The native helper ignores add's boolean, then increments offersFound even on veto.
        return false;
    }
}
