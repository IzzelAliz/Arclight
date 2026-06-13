package io.izzel.arclight.neoforge.mixin.core.world.level.block.entity;

import io.izzel.arclight.neoforge.mod.util.HopperTransferContext;
import io.izzel.arclight.neoforge.mod.util.ResourceHandlerContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ContainerOrHandler;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.event.inventory.HopperInventorySearchEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntity_NeoForge {

    @Inject(method = "getContainerOrHandlerAt", at = @At("RETURN"), cancellable = true)
    private static void arclight$searchDestination(Level level, BlockPos pos, Direction direction, CallbackInfoReturnable<ContainerOrHandler> cir) {
        ContainerOrHandler result = cir.getReturnValue();
        BlockPos hopperPos = pos.relative(direction.getOpposite());
        result = arclight$runSearchEvent(result, CraftBlock.at(level, hopperPos), CraftBlock.at(level, pos), HopperInventorySearchEvent.ContainerType.DESTINATION);
        cir.setReturnValue(result);
        if (result != null && result.itemHandler() != null) {
            HopperTransferContext.push(result);
        }
    }

    @Inject(method = "getSourceContainerOrHandler", at = @At("RETURN"), cancellable = true)
    private static void arclight$searchSource(Level level, Hopper hopper, BlockPos pos, BlockState state, CallbackInfoReturnable<ContainerOrHandler> cir) {
        ContainerOrHandler result = cir.getReturnValue();
        BlockPos hopperPos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY(), hopper.getLevelZ());
        result = arclight$runSearchEvent(result, CraftBlock.at(level, hopperPos), CraftBlock.at(level, pos), HopperInventorySearchEvent.ContainerType.SOURCE);
        cir.setReturnValue(result);
        if (result != null && result.itemHandler() != null) {
            HopperTransferContext.push(result);
        }
    }

    private static ContainerOrHandler arclight$runSearchEvent(ContainerOrHandler containerOrHandler, CraftBlock hopper, CraftBlock searchLocation, HopperInventorySearchEvent.ContainerType containerType) {
        var event = new HopperInventorySearchEvent(HopperTransferContext.toInventory(containerOrHandler), containerType, hopper, searchLocation);
        Bukkit.getServer().getPluginManager().callEvent(event);
        CraftInventory craftInventory = (CraftInventory) event.getInventory();
        if (craftInventory == null) {
            return ContainerOrHandler.EMPTY;
        }
        var nms = craftInventory.getInventory();
        if (nms instanceof ResourceHandlerContainer resourceHandlerContainer) {
            return new ContainerOrHandler(null, resourceHandlerContainer.handler());
        }
        return new ContainerOrHandler(nms, null);
    }
}
