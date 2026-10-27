package io.izzel.arclight.common.mixin.core.world.effect;

import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityEffectTickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.craftbukkit.potion.CraftPotionEffectType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MobEffect.class)
public abstract class MobEffectTickEventMixin {

    @Decorate(method = "applyInstantenousEffect(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/LivingEntity;ID)V",
        require = 1, allow = 1, inject = true, at = @At("HEAD"))
    private void arclight$instantEffectTickEvent(ServerLevel level, Entity source, Entity owner,
                                                LivingEntity target, int amplification, double scale) throws Throwable {
        if (!new EntityEffectTickEvent(
            ((LivingEntityBridge) target).bridge$getBukkitEntity(),
            CraftPotionEffectType.minecraftToBukkit((MobEffect) (Object) this), amplification
        ).callEvent()) {
            DecorationOps.cancel().invoke();
            return;
        }
    }
}
