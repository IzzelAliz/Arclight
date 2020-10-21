package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DropperBlock;
import net.minecraft.dispenser.IDispenseItemBehavior;
import net.minecraft.dispenser.ProxyBlockSource;
import net.minecraft.inventory.DoubleSidedInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.DispenserTileEntity;
import net.minecraft.tileentity.HopperTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryDoubleChest;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DropperBlock.class)
public class DropperBlockMixin {

    @Shadow @Final private static IDispenseItemBehavior DISPENSE_BEHAVIOR;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void dispense(ServerWorld worldIn, BlockPos pos) {
        ProxyBlockSource proxyblocksource = new ProxyBlockSource(worldIn, pos);
        DispenserTileEntity dispensertileentity = proxyblocksource.getBlockTileEntity();
        int i = dispensertileentity.getDispenseSlot();
        if (i < 0) {
            worldIn.playEvent(1001, pos, 0);
        } else {
            ItemStack itemstack = dispensertileentity.getStackInSlot(i);
            if (!itemstack.isEmpty() && net.minecraftforge.items.VanillaInventoryCodeHooks.dropperInsertHook(worldIn, pos, dispensertileentity, i, itemstack)) {
                Direction direction = worldIn.getBlockState(pos).get(DispenserBlock.FACING);
                IInventory iinventory = HopperTileEntity.getInventoryAtPosition(worldIn, pos.offset(direction));
                ItemStack itemstack1;
                if (iinventory == null) {
                    itemstack1 = DISPENSE_BEHAVIOR.dispense(proxyblocksource, itemstack);
                } else {
                    ItemStack split = itemstack.copy().split(1);
                    CraftItemStack craftItemStack = CraftItemStack.asCraftMirror(split);
                    Inventory destinationInventory;
                    // Have to special case large chests as they work oddly
                    if (iinventory instanceof DoubleSidedInventory) {
                        destinationInventory = new CraftInventoryDoubleChest((DoubleSidedInventory) iinventory);
                    } else {
                        destinationInventory = ((IInventoryBridge) iinventory).getOwnerInventory();
                    }
                    InventoryMoveItemEvent event = new InventoryMoveItemEvent(((IInventoryBridge) dispensertileentity).getOwner().getInventory(), craftItemStack, destinationInventory, true);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        return;
                    }
                    itemstack1 = HopperTileEntity.putStackInInventoryAllSlots(dispensertileentity, iinventory, CraftItemStack.asNMSCopy(event.getItem()), direction.getOpposite());
                    if (event.getItem().equals(craftItemStack) && itemstack1.isEmpty()) {
                        itemstack1 = itemstack.copy();
                        itemstack1.shrink(1);
                    } else {
                        itemstack1 = itemstack.copy();
                    }
                }

                dispensertileentity.setInventorySlotContents(i, itemstack1);
            }
        }
    }
}
