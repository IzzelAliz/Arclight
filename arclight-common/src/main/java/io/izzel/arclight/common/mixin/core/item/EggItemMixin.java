package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EggItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EggItem.class)
public abstract class EggItemMixin extends Item {

    public EggItemMixin(Properties properties) {
        super(properties);
    }

    @Redirect(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FF)V"))
    private void arclight$muteSound(World world, PlayerEntity player, double x, double y, double z, SoundEvent soundIn, SoundCategory category, float volume, float pitch) {
    }

    @Eject(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean arclight$updateIfFail(World world, Entity entityIn, CallbackInfoReturnable<ActionResult<ItemStack>> cir, World worldIn, PlayerEntity playerIn, @NotNull Hand handIn) {
        if (!worldIn.addEntity(entityIn)) {
            if (playerIn instanceof ServerPlayerEntityBridge) {
                ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().updateInventory();
            }
            cir.setReturnValue(ActionResult.resultFail(playerIn.getHeldItem(handIn)));
            return false;
        } else {
            worldIn.playSound(null, playerIn.getPosX(), playerIn.getPosY(), playerIn.getPosZ(), SoundEvents.ENTITY_EGG_THROW, SoundCategory.PLAYERS, 0.5F, 0.4F / (random.nextFloat() * 0.4F + 0.8F));
            return true;
        }
    }
}
