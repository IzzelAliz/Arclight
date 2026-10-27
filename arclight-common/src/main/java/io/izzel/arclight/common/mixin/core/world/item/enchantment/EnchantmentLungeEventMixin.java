package io.izzel.arclight.common.mixin.core.world.item.enchantment;

import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityLungeEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Enchantment.class)
public abstract class EnchantmentLungeEventMixin {

    // Dispatch once per matching POST_PIERCING_ATTACK effect, not before condition
    // evaluation. Keep the native effect and the post-listener position read.
    @Decorate(method = "lambda$doPostPiercingAttack$0(Lnet/minecraft/server/level/ServerLevel;ILnet/minecraft/world/item/enchantment/EnchantedItemInUse;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/enchantment/effects/EnchantmentEntityEffect;)V",
        inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private static void arclight$entityLunge(ServerLevel level, int power, EnchantedItemInUse item,
                                            Entity user, EnchantmentEntityEffect effect) throws Throwable {
        if (user instanceof LivingEntity living) {
            EntityLungeEvent event = new EntityLungeEvent(((LivingEntityBridge) living).bridge$getBukkitEntity(), power);
            if (!event.callEvent()) {
                DecorationOps.cancel().invoke();
                return;
            }
            power = event.getLungePower();
            DecorationOps.blackhole().invoke(power);
        }
    }
}
