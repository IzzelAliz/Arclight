package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.AnvilMenuBridge;
import io.izzel.arclight.common.bridge.core.util.IWorldPosCallableBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryAnvil;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.inventory.view.CraftAnvilView;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.view.AnvilView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMixin implements AnvilMenuBridge {

    // @formatter:off
    @Shadow @Final public DataSlot cost;
    @Shadow public int repairItemCountCost;
    @Shadow public String itemName;
    @Shadow public static int calculateIncreasedRepairCost(int oldRepairCost) { return 0; }
    // @formatter:on

    public int cancelThisBySettingCostToMaximum = 40;
    public int maximumRenameCostThreshold = 40;
    public int maximumAllowedRenameCost = 39;
    public int maximumRepairCost = 40;

    private CraftAnvilView bukkitEntity;

    @Decorate(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$prepareAnvilEvent(ResultContainer instance, int i, ItemStack itemStack) throws Throwable {
        var event = new PrepareAnvilEvent((AnvilView) getBukkitView(), CraftItemStack.asCraftMirror(itemStack).clone());
        Bukkit.getServer().getPluginManager().callEvent(event);
        DecorationOps.callsite().invoke(instance, i, CraftItemStack.asNMSCopy(event.getResult()));
    }

    @Inject(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;broadcastChanges()V"))
    private void arclight$sync(CallbackInfo ci) {
        this.sendAllDataToRemote();
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40))
    private int arclight$maximumRepairCost(int i) {
        return i - 40 + maximumRepairCost;
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 39))
    private int arclight$maximumRepairCost2(int i) {
        return i - 40 + maximumRepairCost;
    }

    @Override
    public CraftAnvilView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        var inventory = new CraftInventoryAnvil(
            ((IWorldPosCallableBridge) this.access).bridge$getLocation(), this.inputSlots, this.resultSlots);
        bukkitEntity = new CraftAnvilView(((PlayerEntityBridge) this.player).bridge$getBukkitEntity(), inventory, (AnvilMenu) (Object) this);
        return bukkitEntity;
    }
}
