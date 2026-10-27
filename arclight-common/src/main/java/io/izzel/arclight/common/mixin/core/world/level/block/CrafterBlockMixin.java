package io.izzel.arclight.common.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftInventoryDoubleChest;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.world.level.block.CrafterBlock.class)
public abstract class CrafterBlockMixin {

    private static final String DISPENSE_FROM = "dispenseFrom(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V";
    private static final String DISPENSE_ITEM = "dispenseItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/CrafterBlockEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/crafting/RecipeHolder;)V";

    @WrapOperation(
        method = DISPENSE_FROM,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;assemble(Lnet/minecraft/world/item/crafting/RecipeInput;)Lnet/minecraft/world/item/ItemStack;"),
        require = 1, expect = 1, allow = 1
    )
    private ItemStack arclight$crafterCraftEvent(CraftingRecipe recipe, RecipeInput input,
            Operation<ItemStack> original,
            @Local(argsOnly = true) BlockPos pos,
            @Local(argsOnly = true) ServerLevel level,
            @Local RecipeHolder<CraftingRecipe> recipeHolder,
            @Cancellable org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ItemStack assembled = original.call(recipe, input);
        CrafterCraftEvent event = CraftEventFactory.callCrafterCraftEvent(
            pos, level, assembled, recipeHolder
        );
        if (event.isCancelled()) {
            // Fixed CraftBukkit returns from dispenseFrom here, before tick/state/output/consumption.
            ci.cancel();
            return ItemStack.EMPTY;
        }
        return CraftItemStack.asNMSCopy(event.getResult());
    }

    @WrapOperation(
        method = DISPENSE_ITEM,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;"),
        require = 1, expect = 1, allow = 1
    )
    private ItemStack arclight$crafterContainerMoveEvent(ItemStack results,
            Operation<ItemStack> original,
            @Local(argsOnly = true) CrafterBlockEntity source,
            @Local Container destination,
            @Share("arclight$crafterContainerMoveEvent") LocalRef<InventoryMoveItemEvent> eventRef,
            @Share("arclight$crafterContainerMoveCancelled") LocalBooleanRef cancelled) {
        ItemStack offered = original.call(results);
        if (destination == null) {
            return offered;
        }

        Inventory destinationInventory = destination instanceof CompoundContainer compound
            ? new CraftInventoryDoubleChest(compound)
            : ((IInventoryBridge) destination).getOwnerInventory();
        InventoryMoveItemEvent event = new InventoryMoveItemEvent(
            ((IInventoryBridge) source).getOwnerInventory(),
            CraftItemStack.asCraftMirror(offered),
            destinationInventory,
            true
        );
        Bukkit.getPluginManager().callEvent(event);
        eventRef.set(event);
        cancelled.set(event.isCancelled());
        // Fixed CraftBukkit copies the edited event item before entering either insertion loop;
        // a cancelled event still leaves that edited remainder for the normal spawn tail.
        return CraftItemStack.asNMSCopy(event.getItem());
    }

    @WrapOperation(
        method = DISPENSE_ITEM,
        at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;"),
        require = 1, expect = 1, allow = 1
    )
    private ItemStack arclight$crafterLargeMove(Container source, Container destination, ItemStack stack, Direction direction,
            Operation<ItemStack> original,
            @Share("arclight$crafterContainerMoveCancelled") LocalBooleanRef cancelled) {
        // Fixed CB breaks the one-at-a-time insertion loop on cancellation; return the untouched
        // offer so that the existing output remainder is spawned rather than inserted.
        return cancelled.get() ? stack : original.call(source, destination, stack, direction);
    }

    @WrapOperation(
        method = DISPENSE_ITEM,
        at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;"),
        require = 1, expect = 1, allow = 1
    )
    private ItemStack arclight$crafterNormalMove(Container source, Container destination, ItemStack stack, Direction direction,
            Operation<ItemStack> original,
            @Share("arclight$crafterContainerMoveCancelled") LocalBooleanRef cancelled) {
        // Fixed CB breaks on unchanged count, then its caller spawns the residual output.
        return cancelled.get() ? stack : original.call(source, destination, stack, direction);
    }
}
