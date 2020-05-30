package io.izzel.arclight.common.mixin.core.entity.passive;

import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(CowEntity.class)
public abstract class CowEntityMixin extends AnimalEntityMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean processInteract(final PlayerEntity entityhuman, final Hand enumhand) {
        final ItemStack itemstack = entityhuman.getHeldItem(enumhand);
        if (itemstack.getItem() != Items.BUCKET || entityhuman.abilities.isCreativeMode || this.isChild()) {
            return super.processInteract(entityhuman, enumhand);
        }
        final PlayerBucketFillEvent event = CraftEventFactory.callPlayerBucketFillEvent(entityhuman.world, entityhuman, this.getPosition(), this.getPosition(), null, itemstack, Items.MILK_BUCKET);
        if (event.isCancelled()) {
            return false;
        }
        final ItemStack result = CraftItemStack.asNMSCopy(event.getItemStack());
        entityhuman.playSound(SoundEvents.ENTITY_COW_MILK, 1.0f, 1.0f);
        itemstack.shrink(1);
        if (itemstack.isEmpty()) {
            entityhuman.setHeldItem(enumhand, result);
        } else if (!entityhuman.inventory.addItemStackToInventory(result)) {
            entityhuman.dropItem(result, false);
        }
        return true;
    }
}
