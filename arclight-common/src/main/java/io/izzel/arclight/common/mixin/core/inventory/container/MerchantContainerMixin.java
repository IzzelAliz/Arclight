package io.izzel.arclight.common.mixin.core.inventory.container;

import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.merchant.IMerchant;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.MerchantInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.MerchantContainer;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryMerchant;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantContainer.class)
public abstract class MerchantContainerMixin extends ContainerMixin {

    // @formatter:off
    @Shadow @Final private IMerchant merchant;
    @Shadow @Final private MerchantInventory merchantInventory;
    // @formatter:on

    private CraftInventoryView bukkitEntity = null;
    private PlayerInventory playerInventory;

    @Inject(method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/entity/merchant/IMerchant;)V", at = @At("RETURN"))
    public void arclight$init(int id, PlayerInventory playerInventoryIn, IMerchant merchantIn, CallbackInfo ci) {
        this.playerInventory = playerInventoryIn;
    }

    @Inject(method = "playMerchantYesSound", cancellable = true, at = @At("HEAD"))
    public void arclight$returnIfFail(CallbackInfo ci) {
        if (!(this.merchant instanceof Entity)) {
            ci.cancel();
        }
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity == null) {
            bukkitEntity = new CraftInventoryView(((PlayerEntityBridge) this.playerInventory.player).bridge$getBukkitEntity(), new CraftInventoryMerchant(this.merchant, this.merchantInventory), (Container) (Object) this);
        }
        return bukkitEntity;
    }
}
