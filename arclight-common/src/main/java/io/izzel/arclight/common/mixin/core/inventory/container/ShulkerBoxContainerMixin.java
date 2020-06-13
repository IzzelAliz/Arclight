package io.izzel.arclight.common.mixin.core.inventory.container;

import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ShulkerBoxContainer;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.bukkit.inventory.InventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBoxContainer.class)
public abstract class ShulkerBoxContainerMixin extends ContainerMixin {

    // @formatter:off
    @Shadow @Final private IInventory inventory;
    // @formatter:on

    private CraftInventoryView bukkitEntity;
    private PlayerInventory player;

    @Inject(method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/inventory/IInventory;)V", at = @At("RETURN"))
    public void arclight$init(int p_i50066_1_, PlayerInventory playerInventory, IInventory p_i50066_3_, CallbackInfo ci) {
        this.player = playerInventory;
    }

    @Override
    public InventoryView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        bukkitEntity = new CraftInventoryView(((PlayerEntityBridge) this.player.player).bridge$getBukkitEntity(), new CraftInventory(this.inventory), (Container) (Object) this);
        return bukkitEntity;
    }
}
