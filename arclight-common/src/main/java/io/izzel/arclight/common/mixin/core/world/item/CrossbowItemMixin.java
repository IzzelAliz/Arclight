package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {

    @Decorate(method = "onUseTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CrossbowItem;tryLoadProjectiles(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z"), require = 1)
    private boolean arclight$loadCrossbow(LivingEntity shooter, ItemStack heldItem) throws Throwable {
        var event = new EntityLoadCrossbowEvent(((LivingEntityBridge) shooter).bridge$getBukkitEntity(),
            CraftItemStack.asCraftMirror(heldItem), CraftEquipmentSlot.getHand(shooter.getUsedItemHand()));
        if (!event.callEvent()) {
            return false;
        }

        boolean consume = event.shouldConsumeItem();
        var previous = ArclightCaptures.crossbowNoConsume;
        ArclightCaptures.crossbowNoConsume = consume ? null : com.mojang.datafixers.util.Pair.of(heldItem, shooter);
        try {
            // Keep native projectile selection, draw and charged-component commit, including loader hooks.
            boolean loaded = (boolean) DecorationOps.callsite().invoke(shooter, heldItem);
            // Paper still loads when consume=false, but suppresses the end-of-loading sound.
            return loaded && consume;
        } finally {
            ArclightCaptures.crossbowNoConsume = previous;
        }
    }
}
