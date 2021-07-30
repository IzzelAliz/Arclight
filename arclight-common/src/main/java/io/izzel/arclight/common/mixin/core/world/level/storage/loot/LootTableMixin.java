package io.izzel.arclight.common.mixin.core.world.level.storage.loot;

import io.izzel.arclight.common.bridge.core.world.storage.loot.LootTableBridge;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.world.LootGenerateEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Mixin(LootTable.class)
public abstract class LootTableMixin implements LootTableBridge {

    // @formatter:off
    @Shadow @Final private static Logger LOGGER;
    @Shadow public abstract List<ItemStack> getRandomItems(LootContext context);
    @Shadow protected abstract List<Integer> getAvailableSlots(Container inventory, Random rand);
    @Shadow protected abstract void shuffleAndSplitItems(List<ItemStack> stacks, int emptySlotsCount, Random rand);
    // @formatter:on

    @Eject(method = "fill", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Ljava/util/List;"))
    private List<ItemStack> arclight$nonPluginEvent(LootTable lootTable, LootContext context, CallbackInfo ci, Container inv) {
        List<ItemStack> list = lootTable.getRandomItems(context);
        if (!context.hasParam(LootContextParams.ORIGIN) && !context.hasParam(LootContextParams.THIS_ENTITY)) {
            return list;
        }
        LootGenerateEvent event = CraftEventFactory.callLootGenerateEvent(inv, (LootTable) (Object) this, context, list, false);
        if (event.isCancelled()) {
            ci.cancel();
            return null;
        } else {
            return event.getLoot().stream().map(CraftItemStack::asNMSCopy).collect(Collectors.toList());
        }
    }

    public void fillInventory(Container inv, LootContext context, boolean plugin) {
        List<ItemStack> list = this.getRandomItems(context);
        Random random = context.getRandom();
        LootGenerateEvent event = CraftEventFactory.callLootGenerateEvent(inv, (LootTable) (Object) this, context, list, plugin);
        if (event.isCancelled()) {
            return;
        }
        list = event.getLoot().stream().map(CraftItemStack::asNMSCopy).collect(Collectors.toList());
        List<Integer> list1 = this.getAvailableSlots(inv, random);
        this.shuffleAndSplitItems(list, list1.size(), random);

        for (ItemStack itemstack : list) {
            if (list1.isEmpty()) {
                LOGGER.warn("Tried to over-fill a container");
                return;
            }

            if (itemstack.isEmpty()) {
                inv.setItem(list1.remove(list1.size() - 1), ItemStack.EMPTY);
            } else {
                inv.setItem(list1.remove(list1.size() - 1), itemstack);
            }
        }
    }

    @Override
    public void bridge$fillInventory(Container inv, LootContext context, boolean plugin) {
        this.fillInventory(inv, context, plugin);
    }
}
