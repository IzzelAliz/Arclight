package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.util.IWorldPosCallableBridge;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.craftbukkit.v.inventory.CraftInventorySmithing;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SmithingMenu.class)
public abstract class SmithingTableContainerMixin extends ItemCombinerMixin {

    private CraftInventoryView bukkitEntity;

    @Redirect(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$prepareSmithing(ResultContainer craftResultInventory, int index, ItemStack stack) throws Throwable {
        PrepareSmithingEvent event = new PrepareSmithingEvent(getBukkitView(), CraftItemStack.asCraftMirror(stack).clone());
        Bukkit.getServer().getPluginManager().callEvent(event);
        DecorationOps.callsite().invoke(craftResultInventory, index, CraftItemStack.asNMSCopy(event.getResult()));
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (this.bukkitEntity != null) {
            return this.bukkitEntity;
        }
        CraftInventory inventory = new CraftInventorySmithing(((IWorldPosCallableBridge) this.access).bridge$getLocation(), this.inputSlots, this.resultSlots);
        return this.bukkitEntity = new CraftInventoryView(((PlayerEntityBridge) this.player).bridge$getBukkitEntity(), inventory, (SmithingMenu) (Object) this);
    }
}
