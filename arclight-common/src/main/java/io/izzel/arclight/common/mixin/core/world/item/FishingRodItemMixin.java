package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.event.player.PlayerFishEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FishingRodItem.class)
public class FishingRodItemMixin extends ItemMixin {

    @Decorate(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$fishEvent(Level instance, Entity entity, Level level, Player player, InteractionHand interactionHand) throws Throwable {
        var itemstack = player.getItemInHand(interactionHand);
        PlayerFishEvent playerFishEvent = new PlayerFishEvent((org.bukkit.entity.Player) ((ServerPlayerBridge) player).bridge$getBukkitEntity(), null, (org.bukkit.entity.FishHook) ((EntityBridge) entity).bridge$getBukkitEntity(), CraftEquipmentSlot.getHand(interactionHand), PlayerFishEvent.State.FISHING);
        Bukkit.getPluginManager().callEvent(playerFishEvent);

        if (playerFishEvent.isCancelled()) {
            player.fishing = null;
            return (boolean) DecorationOps.cancel().invoke(InteractionResult.PASS);
        }
        return (boolean) DecorationOps.callsite().invoke(instance, entity);
    }
}
