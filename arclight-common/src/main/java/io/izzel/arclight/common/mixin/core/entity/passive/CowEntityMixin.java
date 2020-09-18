package io.izzel.arclight.common.mixin.core.entity.passive;

import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DrinkHelper;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(CowEntity.class)
public abstract class CowEntityMixin extends AnimalEntityMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public ActionResultType func_230254_b_(PlayerEntity playerEntity, Hand hand) {
        ItemStack itemstack = playerEntity.getHeldItem(hand);
        if (itemstack.getItem() == Items.BUCKET && !this.isChild()) {
            playerEntity.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
            org.bukkit.event.player.PlayerBucketFillEvent event = CraftEventFactory.callPlayerBucketFillEvent((ServerWorld) playerEntity.world, playerEntity, this.getPosition(), this.getPosition(), null, itemstack, Items.MILK_BUCKET);

            if (event.isCancelled()) {
                return ActionResultType.PASS;
            }
            ItemStack itemstack1 = DrinkHelper.fill(itemstack, playerEntity, CraftItemStack.asNMSCopy(event.getItemStack()));
            playerEntity.setHeldItem(hand, itemstack1);
            return ActionResultType.func_233537_a_(this.world.isRemote);
        } else {
            return super.func_230254_b_(playerEntity, hand);
        }
    }
}
