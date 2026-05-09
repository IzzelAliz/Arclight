package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.common.bridge.core.world.inventory.TransientCraftingContainerBridge;
import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.world.inventory.AbstractContainerMenuBridge;
import io.izzel.arclight.common.bridge.core.world.inventory.PosContainerBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryCrafting;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin extends AbstractContainerMenuMixin implements PosContainerBridge {

    // @formatter:off
    @Mutable @Shadow @Final private CraftingContainer craftSlots;
    @Shadow @Final private ResultContainer resultSlots;
    @Accessor("access") public abstract ContainerLevelAccess bridge$getWorldPos();
    // @formatter:on

    private CraftInventoryView<CraftingMenu, ?> bukkitEntity;
    private Inventory playerInventory;

    @Inject(method = "stillValid", cancellable = true, at = @At("HEAD"))
    public void arclight$unreachable(Player playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }

    @Inject(method = "slotsChanged", at = @At("HEAD"))
    public void arclight$capture(Container inventoryIn, CallbackInfo ci) {
        ArclightCaptures.captureWorkbenchContainer((CraftingMenu) (Object) this);
    }

    private static boolean arclight$isRepair;

    @Decorate(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Optional;isPresent()Z"))
    private static boolean arclight$testRepair(Optional<RecipeHolder<CraftingRecipe>> optional, AbstractContainerMenu menu, Level level, Player player, CraftingContainer craftingContainer) throws Throwable {
        ((IInventoryBridge) craftingContainer).setCurrentRecipe(optional.orElse(null));
        arclight$isRepair = optional.map(RecipeHolder::value).orElse(null) instanceof RepairItemRecipe;
        return (boolean) DecorationOps.callsite().invoke(optional);
    }

    @Decorate(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private static void arclight$preCraft(ResultContainer instance, int i, ItemStack itemStack, AbstractContainerMenu abstractContainerMenu, Level level, Player player, CraftingContainer craftingContainer, ResultContainer resultContainer, @Nullable RecipeHolder<CraftingRecipe> recipeHolder,
                                          @Local(ordinal = -1) ItemStack stack) throws Throwable {
        stack = CraftEventFactory.callPreCraftEvent(craftingContainer, instance, itemStack, ((AbstractContainerMenuBridge) abstractContainerMenu).bridge$getBukkitView(), arclight$isRepair);
        DecorationOps.callsite().invoke(instance, i, stack);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("RETURN"))
    public void arclight$init(int i, Inventory playerInventory, ContainerLevelAccess callable, CallbackInfo ci) {
        ((TransientCraftingContainerBridge) this.craftSlots).bridge$setOwner(playerInventory.player);
        ((TransientCraftingContainerBridge) this.craftSlots).bridge$setResultInventory(this.resultSlots);
        this.playerInventory = playerInventory;
    }

    @Override
    public CraftInventoryView<CraftingMenu, ?> getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryCrafting inventory = new CraftInventoryCrafting(this.craftSlots, this.resultSlots);
        bukkitEntity = new CraftInventoryView<>(((PlayerBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (CraftingMenu) (Object) this);
        return bukkitEntity;
    }
}
