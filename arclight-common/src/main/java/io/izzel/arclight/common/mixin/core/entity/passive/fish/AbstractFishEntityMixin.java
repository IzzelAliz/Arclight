package io.izzel.arclight.common.mixin.core.entity.passive.fish;

import io.izzel.arclight.common.mixin.core.entity.CreatureEntityMixin;
import net.minecraft.entity.passive.fish.AbstractFishEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SEntityMetadataPacket;
import net.minecraft.network.play.server.SSpawnMobPacket;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.player.PlayerBucketFishEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(AbstractFishEntity.class)
public abstract class AbstractFishEntityMixin extends CreatureEntityMixin {

    @Inject(method = "getEntityInteractionResult", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "FIELD", ordinal = 0, target = "Lnet/minecraft/world/World;isRemote:Z"))
    private void arclight$fireFishEvent(PlayerEntity playerIn, Hand hand, CallbackInfoReturnable<ActionResultType> cir, ItemStack itemStack, ItemStack itemStack1) {
        PlayerBucketFishEvent event = CraftEventFactory.callPlayerFishBucketEvent((AbstractFishEntity) (Object) this, playerIn, itemStack, itemStack1);
        if (event.isCancelled()) {
            ((ServerPlayerEntity) playerIn).sendContainerToPlayer(playerIn.openContainer);
            ((ServerPlayerEntity) playerIn).connection.sendPacket(new SSpawnMobPacket((AbstractFishEntity) (Object) this));
            ((ServerPlayerEntity) playerIn).connection.sendPacket(new SEntityMetadataPacket(this.getEntityId(), this.dataManager, true));
            itemStack.grow(1);
            cir.setReturnValue(ActionResultType.FAIL);
        } else {
            arclight$bucketItem = CraftItemStack.asNMSCopy(event.getFishBucket());
        }
    }

    private transient ItemStack arclight$bucketItem;

    @ModifyVariable(method = "getEntityInteractionResult", index = 4, at = @At(value = "FIELD", ordinal = 0, target = "Lnet/minecraft/world/World;isRemote:Z"))
    private ItemStack arclight$updateBucketItem(ItemStack itemStack) {
        return arclight$bucketItem;
    }
}
