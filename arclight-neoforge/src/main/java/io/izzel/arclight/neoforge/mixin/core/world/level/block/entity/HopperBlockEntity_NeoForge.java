package io.izzel.arclight.neoforge.mixin.core.world.level.block.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.izzel.arclight.neoforge.mod.util.ResourceHandlerContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ContainerOrHandler;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.event.inventory.HopperInventorySearchEvent;
import org.bukkit.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntity_NeoForge {

    @ModifyExpressionValue(method = "ejectItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;getContainerOrHandlerAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Lnet/neoforged/neoforge/transfer/item/ContainerOrHandler;"))
    private static ContainerOrHandler arclight$searchDestination(ContainerOrHandler found, Level level, BlockPos pos, HopperBlockEntity hopper) {
        return arclight$search(found, level, pos, pos.relative(hopper.facing), HopperInventorySearchEvent.ContainerType.DESTINATION);
    }

    @Inject(method = "getSourceContainerOrHandler", at = @At("RETURN"), cancellable = true, remap = false)
    private static void arclight$searchSource(Level level, Hopper hopper, BlockPos pos, BlockState state, CallbackInfoReturnable<ContainerOrHandler> cir) {
        BlockPos hopperPos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY(), hopper.getLevelZ());
        cir.setReturnValue(arclight$search(cir.getReturnValue(), level, hopperPos, hopperPos.above(), HopperInventorySearchEvent.ContainerType.SOURCE));
    }

    @Unique
    private static ContainerOrHandler arclight$search(ContainerOrHandler found, Level level, BlockPos hopperPos, BlockPos searchPos, HopperInventorySearchEvent.ContainerType type) {
        Inventory inventory = found.container() != null ? new CraftInventory(found.container())
            : found.itemHandler() != null ? ResourceHandlerContainer.getOwnerInventory(found.itemHandler()) : null;
        var event = new HopperInventorySearchEvent(inventory, type, CraftBlock.at(level, hopperPos), CraftBlock.at(level, searchPos));
        Bukkit.getPluginManager().callEvent(event);
        if (event.getInventory() == inventory) {
            return found;
        }
        if (event.getInventory() == null) {
            return ContainerOrHandler.EMPTY;
        }
        var container = ((CraftInventory) event.getInventory()).getInventory();
        return container instanceof ResourceHandlerContainer handler
            ? new ContainerOrHandler(null, handler.getHandler())
            : new ContainerOrHandler(container, null);
    }
}
