package io.izzel.arclight.forge.mod.event;

import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import io.izzel.tools.collection.XmapList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EntityEventDispatcher {

    @SubscribeEvent(receiveCanceled = true)
    public void onLivingDeath(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            // handled at ServerPlayerEntityMixin#onDeath
            // Cancelled at io.izzel.arclight.common.mixin.core.world.entity.LivingEntityMixin#arclight$cancelEvent
            // event.setCanceled(true);
            return;
        }
        LivingEntity livingEntity = event.getEntity();
        Collection<ItemEntity> drops = event.getDrops();
        if (!(drops instanceof ArrayList)) {
            drops = new ArrayList<>(drops);
        }
        List<ItemStack> itemStackList = XmapList.create((List<ItemEntity>) drops, ItemStack.class,
            (ItemEntity entity) -> CraftItemStack.asCraftMirror(entity.getItem()),
            itemStack -> {
                ItemEntity itemEntity = new ItemEntity(livingEntity.level(), livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), CraftItemStack.asNMSCopy(itemStack));
                itemEntity.setDefaultPickUpDelay();
                return itemEntity;
            });
        ArclightEventFactory.callEntityDeathEvent(livingEntity, itemStackList);
        if (drops.isEmpty()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onEntityTame(AnimalTameEvent event) {
        event.setCanceled(CraftEventFactory.callEntityTameEvent(event.getAnimal(), event.getTamer()).isCancelled());
    }
}
