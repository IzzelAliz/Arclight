package io.izzel.arclight.common.mod.server.world.item;

import io.izzel.arclight.common.bridge.bukkit.CraftItemStackBridge;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class EntityDropContainer implements AutoCloseable {

    private final List<ItemStack> allocated = new LinkedList<>();

    public List<ItemStack> initDecorate(List<ItemEntity> items) {
        List<ItemStack> bukkit = new ArrayList<>(items.size());
        for (ItemEntity item : items) {
            CraftItemStack stack = CraftItemStack.asCraftMirror(item.getItem());
            ((CraftItemStackBridge)(Object) stack).arclight$setItemEntity(item);
            allocated.add(stack);
            bukkit.add(stack);
        }
        return bukkit;
    }

    public void convert(List<ItemStack> bukkit, List<ItemEntity> resultContainer, Function<net.minecraft.world.item.ItemStack, ItemEntity> factory) {
        resultContainer.clear();
        forEach(bukkit, factory, resultContainer::add);
    }

    public void forEach(List<ItemStack> bukkit, Function<net.minecraft.world.item.ItemStack, ItemEntity> factory, Consumer<ItemEntity> consumer) {
        for (ItemStack item : bukkit) {
            ItemEntity entity = item instanceof CraftItemStackBridge bridge && bridge.arclight$getItemEntity() instanceof ItemEntity ie ? ie : factory.apply(CraftItemStack.asNMSCopy(item));
            consumer.accept(entity);
        }
    }

    @Override
    public void close() {
        allocated.forEach(stack -> ((CraftItemStackBridge) stack).arclight$setItemEntity(null));
    }
}
