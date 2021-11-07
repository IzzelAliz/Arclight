package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.tileentity.TileEntityBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.event.ForgeEventFactory;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.BrewingStandFuelEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin extends LockableBlockEntityMixin {

    // @formatter:off
    @Shadow private NonNullList<ItemStack> items;
    // @formatter:on

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = MAX_STACK;

    @Eject(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private static void arclight$brewFuel(ItemStack stack, int count, CallbackInfo ci, Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity entity) {
        BrewingStandFuelEvent event = new BrewingStandFuelEvent(CraftBlock.at(level, pos), CraftItemStack.asCraftMirror(stack), 20);
        Bukkit.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            ci.cancel();
        } else {
            entity.fuel = event.getFuelPower();
            if (entity.fuel > 0 && event.isConsuming()) {
                stack.shrink(count);
            }
        }
    }

    /**
     * @author Izzel_Aliz
     * @reason
     */
    @Overwrite
    private static void doBrew(Level level, BlockPos pos, NonNullList<ItemStack> stacks) {
        if (ForgeEventFactory.onPotionAttemptBrew(stacks)) return;
        ItemStack ing = stacks.get(3);

        List<org.bukkit.inventory.ItemStack> brewResults = new ArrayList<>(3);
        for (int i = 0; i < 3; ++i) {
            var input = stacks.get(i);
            var output = BrewingRecipeRegistry.getOutput(input, ing);
            brewResults.add(i, CraftItemStack.asCraftMirror(output.isEmpty() ? input : output));
        }
        BrewingStandBlockEntity entity = ArclightCaptures.getTickingBlockEntity();
        InventoryHolder owner = entity == null ? null : ((TileEntityBridge) entity).bridge$getOwner();
        if (owner != null) {
            BrewEvent event = new BrewEvent(CraftBlock.at(level, pos), (BrewerInventory) owner.getInventory(), brewResults, entity.fuel);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
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

        // BrewingRecipeRegistry.brewPotions(stacks, ing, SLOTS_FOR_SIDES);
        ForgeEventFactory.onPotionBrewed(stacks);
        if (ing.hasContainerItem()) {
            ItemStack containerItem = ing.getContainerItem();
            ing.shrink(1);
            if (ing.isEmpty()) {
                ing = containerItem;
            } else {
                Containers.dropItemStack(level, (double) pos.getX(), (double) pos.getY(), (double) pos.getZ(), containerItem);
            }
        } else ing.shrink(1);

        stacks.set(3, ing);
        level.levelEvent(1035, pos, 0);
    }

    @Override
    public List<ItemStack> getContents() {
        return this.items;
    }

    @Override
    public void onOpen(CraftHumanEntity who) {
        transaction.add(who);
    }

    @Override
    public void onClose(CraftHumanEntity who) {
        transaction.remove(who);
    }

    @Override
    public List<HumanEntity> getViewers() {
        return transaction;
    }

    @Override
    public void setOwner(InventoryHolder owner) {
    }

    @Override
    public int getMaxStackSize() {
        if (maxStack == 0) maxStack = MAX_STACK;
        return maxStack;
    }

    @Override
    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }
}
