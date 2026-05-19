package io.izzel.arclight.common.mixin.core.world.entity.animal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Panda.class)
public abstract class PandaMixin extends AnimalMixin {

    @Shadow private static boolean canPickUpAndEat(ItemEntity itemEntity) {
        return false;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void pickUpItem(ServerLevel level, ItemEntity itemEntity) {
        boolean cancel = this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() && canPickUpAndEat(itemEntity);
        if (!CraftEventFactory.callEntityPickupItemEvent((Panda) (Object) this, itemEntity, 0, cancel).isCancelled()) {
            ItemStack itemstack = itemEntity.getItem();
            this.setItemSlot(EquipmentSlot.MAINHAND, itemstack);
            this.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
            this.take(itemEntity, itemstack.getCount());
            this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.PICKUP);
            itemEntity.discard();
        }

    }
}
