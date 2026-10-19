package io.izzel.arclight.common.mixin.core.world.item;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Sheep;
import org.bukkit.event.entity.SheepDyeWoolEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DyeItem.class)
public class DyeItemMixin {

    @Inject(method = "interactLivingEntity(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"),
        cancellable = true, require = 1, expect = 1, allow = 1)
    private void arclight$sheepDyeWool(ItemStack stack, Player playerIn, LivingEntity target, InteractionHand hand,
                                      CallbackInfoReturnable<InteractionResult> cir, @Local LocalRef<DyeColor> color) {
        // Native guards have accepted the stack's DYE component; veto before sound or mutation.
        byte bColor = (byte) color.get().getId();
        SheepDyeWoolEvent event = new SheepDyeWoolEvent((Sheep) ((LivingEntityBridge) target).bridge$getBukkitEntity(), org.bukkit.DyeColor.getByWoolData(bColor), ((ServerPlayerBridge) playerIn).bridge$getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }
        // Write back the live local, retaining native sound/setColor/shrink calls and their Operations.
        color.set(DyeColor.byId(event.getColor().getWoolData()));
    }
}
