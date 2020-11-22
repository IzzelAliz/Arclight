package io.izzel.arclight.common.mod.server.event;

import com.google.common.collect.Lists;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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
        if (event.getEntityLiving() instanceof ServerPlayerEntity) {
            // handled at ServerPlayerEntityMixin#onDeath
            // Cancelled at io.izzel.arclight.common.mixin.core.entity.LivingEntityMixin#arclight$cancelEvent
            // event.setCanceled(true);
            return;
        }
        LivingEntity livingEntity = event.getEntityLiving();
        Collection<ItemEntity> drops = event.getDrops();
        if (!(drops instanceof ArrayList)) {
            drops = new ArrayList<>(drops);
        }
        List<ItemStack> itemStackList = Lists.transform((List<ItemEntity>) drops,
            (ItemEntity entity) -> CraftItemStack.asCraftMirror(entity.getItem()));
        ArclightEventFactory.callEntityDeathEvent(livingEntity, itemStackList);
        if (drops.isEmpty()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onEntityTame(AnimalTameEvent event) {
        event.setCanceled(CraftEventFactory.callEntityTameEvent(event.getAnimal(), event.getTamer()).isCancelled());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLightningBolt(EntityStruckByLightningEvent event) {
        ArclightCaptures.captureDamageEventEntity(event.getLightning());
    }
}
