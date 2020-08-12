package io.izzel.arclight.common.mixin.core.inventory.container;

import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.inventory.CraftingInventoryBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.text.TranslationTextComponent;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryCrafting;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerContainer.class)
public abstract class PlayerContainerMixin extends ContainerMixin {

    // @formatter:off
    @Shadow @Final private CraftingInventory craftMatrix;
    @Shadow @Final private CraftResultInventory craftResult;
    // @formatter:on

    private CraftInventoryView bukkitEntity;
    private PlayerInventory playerInventory;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$init(PlayerInventory playerInventory, boolean localWorld, PlayerEntity playerIn, CallbackInfo ci) {
        this.playerInventory = playerInventory;
        ((CraftingInventoryBridge) this.craftMatrix).bridge$setOwner(playerInventory.player);
        ((CraftingInventoryBridge) this.craftMatrix).bridge$setResultInventory(this.craftResult);
        this.setTitle(new TranslationTextComponent("container.crafting"));
    }

    @Inject(method = "onCraftMatrixChanged", at = @At("HEAD"))
    public void arclight$captureContainer(IInventory inventoryIn, CallbackInfo ci) {
        ArclightCaptures.captureWorkbenchContainer((Container) (Object) this);
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryCrafting inventory = new CraftInventoryCrafting(this.craftMatrix, this.craftResult);
        bukkitEntity = new CraftInventoryView(((PlayerEntityBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (Container) (Object) this);
        return bukkitEntity;
    }
}
