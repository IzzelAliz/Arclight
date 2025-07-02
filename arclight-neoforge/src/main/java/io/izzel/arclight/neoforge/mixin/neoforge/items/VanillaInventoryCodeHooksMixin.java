package io.izzel.arclight.neoforge.mixin.neoforge.items;

import com.google.common.base.Preconditions;
import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.neoforge.mod.util.DelegatedContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.VanillaInventoryCodeHooks;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.inventory.HopperInventorySearchEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(VanillaInventoryCodeHooks.class)
public abstract class VanillaInventoryCodeHooksMixin {

    @Inject(method = "getItemHandlerAt", at = @At("RETURN"), remap = false)
    private static void arclight$recordResult(Level worldIn, double x, double y, double z, Direction side, CallbackInfoReturnable<Optional<Pair<IItemHandler, Object>>> cir) {
        if (cir.getReturnValue().isPresent()) {
            DelegatedContainer.recordLastHandler();
        }
    }

    @Decorate(method = {"lambda$dropperInsertHook$1", "lambda$insertHook$2", "lambda$insertCrafterOutput$3"}, at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/items/VanillaInventoryCodeHooks;putStackInInventoryAllSlots(Lnet/minecraft/world/level/block/entity/BlockEntity;Ljava/lang/Object;Lnet/neoforged/neoforge/items/IItemHandler;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack arclight$sourceInitiatedMoveItem(BlockEntity source, Object destination, IItemHandler instance, ItemStack stack) throws Throwable {
        if (!stack.isEmpty()) {
            CraftItemStack original = CraftItemStack.asCraftMirror(stack);

            Inventory destInventory = DelegatedContainer.getOwnerInventory(destination, instance);
            InventoryMoveItemEvent event = new InventoryMoveItemEvent(((IInventoryBridge) source).getOwnerInventory(), original.clone(), destInventory, true);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                if (source instanceof HopperBlockEntity hopper) {
                    hopper.setCooldown(((WorldBridge) source.getLevel()).bridge$spigotConfig().hopperTransfer);
                }
                // Delay hopper checks
                // Arclight: we can return stack directly so we use vanilla revert logic and eventually return false if none is transferred
                // Arclight: but CraftBukkit makes it delayed directly, don't know why, so have to catch the index to revert change?
                return stack;
            }
            stack = CraftItemStack.asNMSCopy(event.getItem());
        }
        return (ItemStack) DecorationOps.callsite().invoke(source, destination, instance, stack);
    }

    @Decorate(method = "lambda$extractHook$0", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/neoforged/neoforge/items/IItemHandler;extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack arclight$nonSourceInitiatedMoveItem(IItemHandler instance, int slot, int expected, boolean simulate, Hopper hopper, Pair<IItemHandler, Object> sourcePair) throws Throwable {
        Preconditions.checkArgument(simulate, "Should be injected at simulate=true");
        ItemStack stack = (ItemStack) DecorationOps.callsite().invoke(instance, slot, expected, simulate);
        if (stack.isEmpty()) {
            return stack;
        }
        Object destination = sourcePair.getRight();
        CraftItemStack original = CraftItemStack.asCraftMirror(stack);
        Inventory sourceInventory = DelegatedContainer.getOwnerInventory(destination, instance);

        InventoryMoveItemEvent event = new InventoryMoveItemEvent(sourceInventory, original.clone(), ((IInventoryBridge) hopper).getOwnerInventory(), false);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            if (hopper instanceof HopperBlockEntity entity) {
                entity.setCooldown(((WorldBridge) entity.getLevel()).bridge$spigotConfig().hopperTransfer);
            }
            // Delay hopper checks
            // Arclight: we can return stack directly so we use vanilla revert logic and eventually return false if none is transferred
            // Arclight: but CraftBukkit makes it delayed directly, don't know why, so have to catch the index to revert change?
            return ItemStack.EMPTY;
        }
        return CraftItemStack.asNMSCopy(event.getItem());
    }

    private static Optional<Pair<IItemHandler, Object>> arclight$runHopperInventorySearchEvent(Container inventory, CraftBlock hopper, CraftBlock searchLocation, HopperInventorySearchEvent.ContainerType containerType) {
        var event = new HopperInventorySearchEvent((inventory != null) ? new CraftInventory(inventory) : null, containerType, hopper, searchLocation);
        Bukkit.getServer().getPluginManager().callEvent(event);
        CraftInventory craftInventory = (CraftInventory) event.getInventory();
        return Optional.ofNullable(DelegatedContainer.makeItemHandlerPair(craftInventory));
    }

    @Decorate(method = "insertHook", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/items/VanillaInventoryCodeHooks;getAttachedItemHandler(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Ljava/util/Optional;"))
    private static Optional<Pair<IItemHandler, Object>> arclight$searchTo(Level level, BlockPos pos, Direction direction, HopperBlockEntity hopperBlockEntity) throws Throwable {
        final var handler = (Optional<Pair<IItemHandler, Object>>) DecorationOps.callsite().invoke(level, pos, direction);
        final var hopper = CraftBlock.at(level, pos);
        final var searchBlock = CraftBlock.at(level, pos.relative(hopperBlockEntity.facing));
        final var container = handler.map(DelegatedContainer::new).orElse(null);
        return arclight$runHopperInventorySearchEvent(container, hopper, searchBlock, HopperInventorySearchEvent.ContainerType.DESTINATION);
    }

    @Decorate(method = "extractHook", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/items/VanillaInventoryCodeHooks;getSourceItemHandler(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/Hopper;)Ljava/util/Optional;"))
    private static Optional<Pair<IItemHandler, Object>> arclight$searchFrom(Level level, Hopper hopper) throws Throwable {
        final var handler = (Optional<Pair<IItemHandler, Object>>) DecorationOps.callsite().invoke(level, hopper);
        final var blockPos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY(), hopper.getLevelZ());
        final var hopperBlock = CraftBlock.at(level, blockPos);
        final var containerBlock = CraftBlock.at(level, blockPos.above());
        final var container = handler.map(DelegatedContainer::new).orElse(null);
        return arclight$runHopperInventorySearchEvent(container, hopperBlock, containerBlock, HopperInventorySearchEvent.ContainerType.SOURCE);
    }

}
