package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.AnvilMenuBridge;
import io.izzel.arclight.common.bridge.core.util.IWorldPosCallableBridge;
import io.izzel.arclight.common.mod.server.world.inventory.ArclightAnvilView;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryAnvil;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.inventory.view.CraftAnvilView;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    private boolean arclight$zeroCostAllowed = false;

    @Override
    public void arclight$allowZeroCost() {
        arclight$zeroCostAllowed = true;
    }

    @Override
    public boolean arclight$isZeroCostAllowed() {
        return arclight$zeroCostAllowed;
    }

    @Decorate(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$prepareAnvilEvent(ResultContainer instance, int i, ItemStack itemStack) throws Throwable {
        arclight$zeroCostAllowed = false;
        final CraftAnvilView craft = getBukkitView();
        if (craft.getClass() == ArclightAnvilView.class) {
            // Call anvil event; preserve injection point
            var event = new PrepareAnvilEvent(craft, CraftItemStack.asCraftMirror(itemStack).clone());
            Bukkit.getServer().getPluginManager().callEvent(event);
            DecorationOps.callsite().invoke(instance, i, CraftItemStack.asNMSCopy(event.getResult()));
        } else {
            // Run plugin custom logic
            CraftEventFactory.callPrepareAnvilEvent(craft, itemStack);
        }
    }

    @Decorate(method = "mayPickup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/DataSlot;get()I"))
    private int arclight$tryAllowZeroCost(DataSlot instance) throws Throwable {
        final int value = (int) DecorationOps.callsite().invoke(instance);
        if (value == 0 && arclight$zeroCostAllowed) {
            return 1;
        }
        return value;
    }

    @Inject(method = "mayPickup", at = @At("RETURN"), cancellable = true)
    private void arclight$considerFlag(Player player, boolean hasItem, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(cir.getReturnValueZ() && hasItem);
    }

    @Inject(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;broadcastChanges()V"))
    private void arclight$sync(CallbackInfo ci) {
        this.sendAllDataToRemote();
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40), require = 0)
    private int arclight$maximumRepairCost(int i) {
        return i - 40 + maximumRepairCost;
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 39), require = 0)
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
        bukkitEntity = new ArclightAnvilView(((PlayerEntityBridge) this.player).bridge$getBukkitEntity(), inventory, (AnvilMenu) (Object) this);
        return bukkitEntity;
    }
}
