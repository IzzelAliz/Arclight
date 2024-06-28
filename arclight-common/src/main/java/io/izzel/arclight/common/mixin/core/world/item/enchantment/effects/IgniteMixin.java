package io.izzel.arclight.common.mixin.core.world.item.enchantment.effects;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.Ignite;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Ignite.class)
public class IgniteMixin {

    @Shadow @Final private LevelBasedValue duration;

    @Decorate(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;igniteForSeconds(F)V"))
    private void arclight$combustEvent(Entity instance, float f, ServerLevel serverLevel, int i, EnchantedItemInUse enchantedItemInUse) throws Throwable {
        EntityCombustEvent entityCombustEvent;
        if (enchantedItemInUse.owner() != null) {
            entityCombustEvent = new EntityCombustByEntityEvent(enchantedItemInUse.owner().bridge$getBukkitEntity(), instance.bridge$getBukkitEntity(), this.duration.calculate(i));
        } else {
            entityCombustEvent = new EntityCombustEvent(instance.bridge$getBukkitEntity(), this.duration.calculate(i));
        }

        Bukkit.getPluginManager().callEvent(entityCombustEvent);
        if (entityCombustEvent.isCancelled()) {
            return;
        }

        DecorationOps.callsite().invoke(instance, f);
    }
}
