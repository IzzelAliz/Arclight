package io.izzel.arclight.common.mixin.core.world.entity.animal;

import com.destroystokyo.paper.event.entity.EntityJumpEvent;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityToggleSitEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Panda.class)
public abstract class PandaMixin extends AnimalMixin {

    @Shadow private static boolean canPickUpAndEat(ItemEntity itemEntity) {
        return false;
    }

    @Decorate(method = "sit(Z)V", inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$toggleSit(boolean value) throws Throwable {
        if (!new EntityToggleSitEvent(this.getBukkitEntity(), value).callEvent()) {
            DecorationOps.cancel().invoke();
            return;
        }
    }

    @Decorate(method = "afterSneeze()V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/animal/panda/Panda;jumpFromGround()V"), require = 1, allow = 1)
    private void arclight$entityJump(Panda panda) throws Throwable {
        // Paper uses the sneezer for the event and veto, but the nearby panda for the jump.
        if (this.level().isClientSide() || new EntityJumpEvent(this.getBukkitEntity()).callEvent()) {
            DecorationOps.callsite().invoke(panda);
        } else {
            ((Panda) (Object) this).setJumping(false);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void pickUpItem(ServerLevel level, ItemEntity itemEntity) {
        boolean cancel = !(this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() && canPickUpAndEat(itemEntity));
        if (!CraftEventFactory.callEntityPickupItemEvent((Panda) (Object) this, itemEntity, 0, cancel).isCancelled()) {
            ((Panda) (Object) this).onItemPickup(itemEntity);
            ItemStack itemstack = itemEntity.getItem();
            this.setItemSlot(EquipmentSlot.MAINHAND, itemstack);
            this.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
            this.take(itemEntity, itemstack.getCount());
            itemEntity.bridge().bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.PICKUP);
            itemEntity.discard();
        }

    }
}
