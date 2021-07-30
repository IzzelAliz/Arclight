package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Sheep;
import org.bukkit.event.entity.SheepDyeWoolEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DyeItem.class)
public class DyeItemMixin {

    // @formatter:off
    @Shadow @Final private DyeColor dyeColor;
    // @formatter:on

    @Eject(method = "interactLivingEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Sheep;setColor(Lnet/minecraft/world/item/DyeColor;)V"))
    private void arclight$sheepDyeWool(net.minecraft.world.entity.animal.Sheep sheepEntity, DyeColor color, CallbackInfoReturnable<Boolean> cir, ItemStack stack, Player playerIn, LivingEntity target, InteractionHand hand) {
        byte bColor = (byte) this.dyeColor.getId();
        SheepDyeWoolEvent event = new SheepDyeWoolEvent((Sheep) ((LivingEntityBridge) target).bridge$getBukkitEntity(), org.bukkit.DyeColor.getByWoolData(bColor), ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        } else {
            sheepEntity.setColor(DyeColor.byId(event.getColor().getWoolData()));
        }
    }
}
