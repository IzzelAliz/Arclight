package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.CraftingInventoryBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryCrafting;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryMenu.class)
public abstract class PlayerContainerMixin extends AbstractContainerMenuMixin {

    // @formatter:off
    @Shadow @Final private CraftingContainer craftSlots;
    @Shadow @Final private ResultContainer resultSlots;
    // @formatter:on

    private CraftInventoryView bukkitEntity;
    private Inventory playerInventory;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$init(Inventory playerInventory, boolean localWorld, Player playerIn, CallbackInfo ci) {
        this.playerInventory = playerInventory;
        ((CraftingInventoryBridge) this.craftSlots).bridge$setOwner(playerInventory.player);
        ((CraftingInventoryBridge) this.craftSlots).bridge$setResultInventory(this.resultSlots);
        this.setTitle(new TranslatableComponent("container.crafting"));
    }

    @Inject(method = "slotsChanged", at = @At("HEAD"))
    public void arclight$captureContainer(Container inventoryIn, CallbackInfo ci) {
        ArclightCaptures.captureWorkbenchContainer((AbstractContainerMenu) (Object) this);
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryCrafting inventory = new CraftInventoryCrafting(this.craftSlots, this.resultSlots);
        bukkitEntity = new CraftInventoryView(((PlayerEntityBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (AbstractContainerMenu) (Object) this);
        return bukkitEntity;
    }
}
