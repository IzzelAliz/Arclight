package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Sheep;
import org.bukkit.event.entity.SheepDyeWoolEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DyeItem.class)
public class DyeItemMixin {

    // @formatter:off
    @Shadow @Final private DyeColor dyeColor;
    // @formatter:on

    @Inject(method = "itemInteractionForEntity", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/SheepEntity;setFleeceColor(Lnet/minecraft/item/DyeColor;)V"))
    public void arclight$sheepDyeWool(ItemStack stack, PlayerEntity playerIn, LivingEntity target, Hand hand, CallbackInfoReturnable<Boolean> cir) {
        byte bColor = (byte) this.dyeColor.getId();
        SheepDyeWoolEvent event = new SheepDyeWoolEvent((Sheep) ((LivingEntityBridge) target).bridge$getBukkitEntity(), org.bukkit.DyeColor.getByWoolData(bColor));
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
        arclight$capturedColor = DyeColor.byId(event.getColor().getWoolData());
    }

    private transient DyeColor arclight$capturedColor;

    @Redirect(method = "itemInteractionForEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/SheepEntity;setFleeceColor(Lnet/minecraft/item/DyeColor;)V"))
    public void arclight$setColor(SheepEntity sheepEntity, DyeColor color) {
        sheepEntity.setFleeceColor(arclight$capturedColor);
        arclight$capturedColor = null;
    }
}
