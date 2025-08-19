package io.izzel.arclight.common.mod.server.world.item;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.izzel.tools.collection.XmapList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;

import java.util.List;

public class ArclightItemStack {

    private static final BiMap<ItemEntity, org.bukkit.inventory.ItemStack> ALLOCATED = HashBiMap.create();

    public static XmapList<ItemEntity, org.bukkit.inventory.ItemStack> initDecorate(LivingEntity living, List<ItemEntity> items) {
        items.forEach(item -> ALLOCATED.put(item, CraftItemStack.asCraftMirror(item.getItem())));
        return XmapList.create(
                items, org.bukkit.inventory.ItemStack.class,
                entity -> ALLOCATED.computeIfAbsent(entity, ct -> CraftItemStack.asCraftMirror(entity.getItem())),
                stack -> ALLOCATED.inverse().computeIfAbsent(stack, craft -> spawnAt(living, CraftItemStack.asNMSCopy(craft)))
        );
    }

    public static ItemEntity spawnAt(LivingEntity entity, ItemStack stack) {
        final var result = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack);
        result.setDefaultPickUpDelay();
        return result;
    }

    public static void cleanup() {
        ALLOCATED.clear();
    }
}
