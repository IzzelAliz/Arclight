package io.izzel.arclight.fabric.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.tileentity.TileEntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.List;

@Mixin(BrewingStandBlockEntity.class)
public class BrewingStandBlockEntityMixin_Fabric {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private static void doBrew(Level level, BlockPos pos, NonNullList<ItemStack> stacks) {
        ItemStack itemStack = stacks.get(3);

        if (arclight$brewEvent(level, pos, stacks, itemStack)) {
            return;
        }

        itemStack.shrink(1);
        if (itemStack.getItem().hasCraftingRemainingItem()) {
            ItemStack itemStack2 = new ItemStack(itemStack.getItem().getCraftingRemainingItem());
            if (itemStack.isEmpty()) {
                itemStack = itemStack2;
            } else {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), itemStack2);
            }
        }

        stacks.set(3, itemStack);
        level.levelEvent(1035, pos, 0);
    }

    private static boolean arclight$brewEvent(Level level, BlockPos pos, NonNullList<ItemStack> stacks, ItemStack ing) {
        List<org.bukkit.inventory.ItemStack> brewResults = new ArrayList<>(3);
        for (int i = 0; i < 3; ++i) {
            var input = stacks.get(i);
            var output = ((WorldBridge) level).bridge$forge$potionBrewMix(input, ing);
            brewResults.add(i, CraftItemStack.asCraftMirror(output.isEmpty() ? input : output));
        }
        BrewingStandBlockEntity entity = ArclightCaptures.getTickingBlockEntity();
        InventoryHolder owner = entity == null ? null : ((TileEntityBridge) entity).bridge$getOwner();
        if (owner != null) {
            BrewEvent event = new BrewEvent(CraftBlock.at(level, pos), (BrewerInventory) owner.getInventory(), brewResults, entity.fuel);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return true;
            } else {
                for (int i = 0; i < 3; ++i) {
                    if (i < brewResults.size()) {
                        stacks.set(i, CraftItemStack.asNMSCopy(brewResults.get(i)));
                    } else {
                        stacks.set(i, ItemStack.EMPTY);
                    }
                }
            }
        }
        return false;
    }
}
