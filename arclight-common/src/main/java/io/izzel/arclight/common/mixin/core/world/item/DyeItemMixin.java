package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
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

@Mixin(DyeItem.class)
public class DyeItemMixin {

    @Decorate(method = "interactLivingEntity(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/sheep/Sheep;setColor(Lnet/minecraft/world/item/DyeColor;)V"),
        require = 1, expect = 1, allow = 1)
    private void arclight$sheepDyeWool(net.minecraft.world.entity.animal.sheep.Sheep sheep, DyeColor color,
                                     ItemStack stack, Player playerIn, LivingEntity target, InteractionHand hand) throws Throwable {
        // Fixed Paper dispatches after the native sound, inside the server mutation branch.
        byte bColor = (byte) color.getId();
        SheepDyeWoolEvent event = new SheepDyeWoolEvent((Sheep) ((LivingEntityBridge) sheep).bridge$getBukkitEntity(), org.bukkit.DyeColor.getByWoolData(bColor), ((ServerPlayerBridge) playerIn).bridge$getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.PASS);
            return;
        }
        // The original outer caller still owns shrink and SUCCESS; preserve the supplied setter continuation.
        DecorationOps.callsite().invoke(sheep, DyeColor.byId(event.getColor().getWoolData()));
    }
}
