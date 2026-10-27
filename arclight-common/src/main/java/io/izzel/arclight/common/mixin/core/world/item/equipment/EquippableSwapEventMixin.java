package io.izzel.arclight.common.mixin.core.world.item.equipment;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.player.PlayerSwapWithEquipmentSlotEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Equippable.class)
public abstract class EquippableSwapEventMixin {

    @Decorate(method = "swapWithEquipmentSlot(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameComponents(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"),
        require = 1, allow = 1)
    private boolean arclight$playerSwapWithEquipmentSlot(ItemStack first, ItemStack inEquipmentSlot,
                                                       ItemStack inHand, Player player) throws Throwable {
        boolean sameItems = (boolean) DecorationOps.callsite().invoke(first, inEquipmentSlot);
        if (!sameItems) {
            // The slot/type and binding-enchantment checks have already passed.
            // Snapshot both items before the native statistics and stack mutations.
            var bukkitPlayer = (org.bukkit.entity.Player) ((PlayerBridge) player).bridge$getBukkitEntity();
            var event = new PlayerSwapWithEquipmentSlotEvent(bukkitPlayer,
                CraftItemStack.asBukkitCopy(inHand),
                CraftEquipmentSlot.getSlot(((Equippable) (Object) this).slot()),
                CraftItemStack.asBukkitCopy(inEquipmentSlot));
            if (!event.callEvent()) {
                bukkitPlayer.updateInventory();
                return (boolean) DecorationOps.cancel().invoke((InteractionResult) InteractionResult.FAIL);
            }
        }
        return sameItems;
    }
}
