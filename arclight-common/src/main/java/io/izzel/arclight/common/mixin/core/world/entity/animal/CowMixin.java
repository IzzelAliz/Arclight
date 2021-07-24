package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.mixin.core.world.entity.animal.AnimalMixin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Cow.class)
public abstract class CowMixin extends AnimalMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public InteractionResult mobInteract(Player playerEntity, InteractionHand hand) {
        ItemStack itemstack = playerEntity.getItemInHand(hand);
        if (itemstack.getItem() == Items.BUCKET && !this.isBaby()) {
            playerEntity.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
            org.bukkit.event.player.PlayerBucketFillEvent event = CraftEventFactory.callPlayerBucketFillEvent((ServerLevel) playerEntity.level, playerEntity, this.blockPosition(), this.blockPosition(), null, itemstack, Items.MILK_BUCKET);

            if (event.isCancelled()) {
                return InteractionResult.PASS;
            }
            ItemStack itemstack1 = ItemUtils.createFilledResult(itemstack, playerEntity, CraftItemStack.asNMSCopy(event.getItemStack()));
            playerEntity.setItemInHand(hand, itemstack1);
            return InteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            return super.mobInteract(playerEntity, hand);
        }
    }
}
