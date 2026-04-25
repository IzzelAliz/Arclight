package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryMerchant;
import org.bukkit.craftbukkit.v.inventory.view.CraftMerchantView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin extends AbstractContainerMenuMixin {

    @Shadow @Final private MerchantContainer tradeContainer;

    private CraftMerchantView bukkitEntity;
    private Inventory playerInventory;
    private Merchant arclight$merchant;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V", at = @At("RETURN"))
    private void arclight$init(int i, Inventory inventory, Merchant merchant, CallbackInfo ci) {
        this.playerInventory = inventory;
        this.arclight$merchant = merchant;
    }

    @Override
    public CraftMerchantView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryMerchant inventory = new CraftInventoryMerchant(this.arclight$merchant, this.tradeContainer);
        bukkitEntity = new CraftMerchantView(((PlayerBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (MerchantMenu) (Object) this, this.arclight$merchant);
        return bukkitEntity;
    }
}
