package io.izzel.arclight.neoforge.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityDamageItemEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public abstract class ItemStackNaturalDamageEventMixin_NeoForge {

    @Decorate(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;processDurabilityChange(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)I"))
    private int arclight$naturalOwnerItemDamage(ItemStack stack, int amount, ServerLevel level, LivingEntity owner) throws Throwable {
        int result = (int) DecorationOps.callsite().invoke(stack, amount, level, owner);
        // Scoped API/equipment/enchantment calls already use the retained consumer.
        // This covers Neo's direct LivingEntity overload without changing player events.
        if (result > 0 && owner != null && !(owner instanceof ServerPlayer) && ArclightCaptures.getItemDamageOwner(stack, owner) == null) {
            EntityDamageItemEvent event = new EntityDamageItemEvent(((LivingEntityBridge) owner).bridge$getBukkitEntity(),
                CraftItemStack.asCraftMirror(stack), result);
            return event.callEvent() ? event.getDamage() : 0;
        }
        return result;
    }
}
