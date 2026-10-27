package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.event.player.PlayerFishEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FishingRodItem.class)
public class FishingRodItemMixin extends ItemMixin {

    @Decorate(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/FishingHook;retrieve(Lnet/minecraft/world/item/ItemStack;)I"))
    private int arclight$retrieveHand(net.minecraft.world.entity.projectile.FishingHook hook, ItemStack stack, Level level, Player player, InteractionHand hand) throws Throwable {
        var bridge = (io.izzel.arclight.common.bridge.core.world.entity.projectile.FishingHookBridge) hook;
        var previous = bridge.bridge$swapRetrieveHand(hand);
        try {
            return (int) DecorationOps.callsite().invoke(hook, stack);
        } finally {
            bridge.bridge$swapRetrieveHand(previous);
        }
    }


    @Decorate(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectile(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/projectile/Projectile;"))
    private Projectile arclight$fishEvent(Projectile entity, ServerLevel serverLevel, ItemStack itemStack, Level level, Player player, InteractionHand interactionHand) throws Throwable {
        PlayerFishEvent playerFishEvent = new PlayerFishEvent((org.bukkit.entity.Player) player.bridge$getBukkitEntity(), null, (org.bukkit.entity.FishHook) entity.bridge$getBukkitEntity(), CraftEquipmentSlot.getHand(interactionHand), PlayerFishEvent.State.FISHING);
        Bukkit.getPluginManager().callEvent(playerFishEvent);

        if (playerFishEvent.isCancelled()) {
            player.fishing = null;
            return (Projectile) DecorationOps.cancel().invoke((InteractionResult) InteractionResult.PASS);
        }
        return (Projectile) DecorationOps.callsite().invoke(entity, serverLevel, itemStack);
    }
}
