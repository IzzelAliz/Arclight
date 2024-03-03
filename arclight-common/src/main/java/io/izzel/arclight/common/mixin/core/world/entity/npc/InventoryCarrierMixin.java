package io.izzel.arclight.common.mixin.core.world.entity.npc;

import io.izzel.arclight.common.mod.server.ArclightContainer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(InventoryCarrier.class)
public interface InventoryCarrierMixin {

    // @formatter:off
    @Shadow SimpleContainer getInventory();
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    static void pickUpItem(Mob mob, InventoryCarrier carrier, ItemEntity itemEntity) {
        ItemStack itemstack = itemEntity.getItem();
        if (mob.wantsToPickUp(itemstack)) {
            SimpleContainer simplecontainer = carrier.getInventory();
            boolean flag = simplecontainer.canAddItem(itemstack);
            if (!flag) {
                return;
            }

            var remaining = ArclightContainer.copyOf(carrier.getInventory()).addItem(itemstack);
            if (CraftEventFactory.callEntityPickupItemEvent(mob, itemEntity, remaining.getCount(), false).isCancelled()) {
                return;
            }
            mob.onItemPickup(itemEntity);
            int i = itemstack.getCount();
            ItemStack itemstack1 = simplecontainer.addItem(itemstack);
            mob.take(itemEntity, i - itemstack1.getCount());
            if (itemstack1.isEmpty()) {
                itemEntity.bridge().bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.PICKUP);
                itemEntity.discard();
            } else {
                itemstack.setCount(itemstack1.getCount());
            }
        }

    }
}
