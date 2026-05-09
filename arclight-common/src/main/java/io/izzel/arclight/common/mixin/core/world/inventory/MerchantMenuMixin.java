package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import net.minecraft.world.entity.Entity;
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

    @Shadow @Final private Merchant trader;
    @Shadow @Final private MerchantContainer tradeContainer;

    private CraftMerchantView bukkitEntity;
    private Inventory inventory;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V", at = @At("RETURN"))
    public void arclight$init(int id, Inventory inventory, Merchant merchant, CallbackInfo ci) {
        this.inventory = inventory;
    }

    @Inject(method = "playTradeSound", cancellable = true, at = @At("HEAD"))
    public void arclight$returnIfFail(CallbackInfo ci) {
        if (!(this.trader instanceof Entity)) {
            ci.cancel();
        }
    }

    @Override
    public CraftMerchantView getBukkitView() {
        if (bukkitEntity == null) {
            bukkitEntity = new CraftMerchantView(((PlayerBridge) this.inventory.player).bridge$getBukkitEntity(), new CraftInventoryMerchant(this.trader, this.tradeContainer), (MerchantMenu) (Object) this, trader);
        }
        return bukkitEntity;
    }
}
