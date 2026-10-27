package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.player.PlayerMapFilledEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EmptyMapItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EmptyMapItem.class)
public abstract class EmptyMapItemMixin {

    @Decorate(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"), require = 1, allow = 1)
    private void arclight$captureEmptyMap(ItemStack stack, int amount, LivingEntity consumer,
                                         @Local(allocate = "arclightOriginalEmptyMap") org.bukkit.inventory.ItemStack originalItem) throws Throwable {
        originalItem = CraftItemStack.asBukkitCopy(stack);
        DecorationOps.blackhole().invoke(originalItem);
        DecorationOps.callsite().invoke(stack, amount, consumer);
    }

    @Decorate(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/MapItem;create(Lnet/minecraft/server/level/ServerLevel;IIBZZ)Lnet/minecraft/world/item/ItemStack;"), require = 1, allow = 1)
    private ItemStack arclight$playerMapFilled(ServerLevel serverLevel, int x, int z, byte scale, boolean trackingPosition, boolean unlimitedTracking,
                                              Level level, Player player, InteractionHand hand,
                                              @Local(allocate = "arclightOriginalEmptyMap") org.bukkit.inventory.ItemStack originalItem) throws Throwable {
        ItemStack map = (ItemStack) DecorationOps.callsite().invoke(serverLevel, x, z, scale, trackingPosition, unlimitedTracking);
        var event = new PlayerMapFilledEvent((org.bukkit.entity.Player) ((PlayerBridge) player).bridge$getBukkitEntity(),
            originalItem, CraftItemStack.asCraftMirror(map));
        event.callEvent();
        // The native hand replacement, inventory insertion and overflow drop all use this result.
        return CraftItemStack.unwrap(event.getCreatedMap());
    }
}
