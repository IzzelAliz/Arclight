package io.izzel.arclight.common.mixin.core.entity.passive;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Predicate;

@Mixin(PandaEntity.class)
public abstract class PandaEntityMixin extends AnimalEntityMixin {

    @Shadow @Final private static Predicate<ItemEntity> PANDA_ITEMS;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void updateEquipmentIfNeeded(ItemEntity itemEntity) {
        boolean cancel = this.getItemStackFromSlot(EquipmentSlotType.MAINHAND).isEmpty() && PANDA_ITEMS.test(itemEntity);
        if (!CraftEventFactory.callEntityPickupItemEvent((PandaEntity) (Object) this, itemEntity, 0, cancel).isCancelled()) {
            ItemStack itemstack = itemEntity.getItem();
            this.setItemStackToSlot(EquipmentSlotType.MAINHAND, itemstack);
            this.inventoryHandsDropChances[EquipmentSlotType.MAINHAND.getIndex()] = 2.0F;
            this.onItemPickup(itemEntity, itemstack.getCount());
            itemEntity.remove();
        }

    }
}
