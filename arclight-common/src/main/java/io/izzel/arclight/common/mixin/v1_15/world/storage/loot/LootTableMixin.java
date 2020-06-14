package io.izzel.arclight.common.mixin.v1_15.world.storage.loot;

import io.izzel.arclight.common.bridge.world.storage.loot.LootTableBridge;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.world.LootGenerateEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Mixin(LootTable.class)
public abstract class LootTableMixin implements LootTableBridge {

    // @formatter:off
    @Shadow @Final private static Logger LOGGER;
    @Shadow public abstract List<ItemStack> generate(LootContext context);
    @Shadow protected abstract List<Integer> getEmptySlotsRandomized(IInventory inventory, Random rand);
    @Shadow protected abstract void shuffleItems(List<ItemStack> stacks, int p_186463_2_, Random rand);
    // @formatter:on

    public void fillInventory(IInventory inv, LootContext context, boolean plugin) {
        List<ItemStack> list = this.generate(context);
        Random random = context.getRandom();
        LootGenerateEvent event = CraftEventFactory.callLootGenerateEvent(inv, (LootTable) (Object) this, context, list, plugin);
        if (event.isCancelled()) {
            return;
        }
        list = event.getLoot().stream().map(CraftItemStack::asNMSCopy).collect(Collectors.toList());
        List<Integer> list1 = this.getEmptySlotsRandomized(inv, random);
        this.shuffleItems(list, list1.size(), random);

        for (ItemStack itemstack : list) {
            if (list1.isEmpty()) {
                LOGGER.warn("Tried to over-fill a container");
                return;
            }

            if (itemstack.isEmpty()) {
                inv.setInventorySlotContents(list1.remove(list1.size() - 1), ItemStack.EMPTY);
            } else {
                inv.setInventorySlotContents(list1.remove(list1.size() - 1), itemstack);
            }
        }
    }

    @Override
    public void bridge$fillInventory(IInventory inv, LootContext context, boolean plugin) {
        this.fillInventory(inv, context, plugin);
    }
}
