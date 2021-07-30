package io.izzel.arclight.common.mixin.core.server.commands;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.EffectCommands;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(EffectCommands.class)
public class EffectCommandMixin {

    @Inject(method = "giveEffect", at = @At("HEAD"))
    private static void arclight$addReason(CommandSourceStack source, Collection<? extends Entity> targets, MobEffect effect, Integer seconds, int amplifier, boolean showParticles, CallbackInfoReturnable<Integer> cir) {
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity) {
                ((LivingEntityBridge) entity).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.COMMAND);
            }
        }
    }

    @Inject(method = "clearEffects", at = @At("HEAD"))
    private static void arclight$removeAllReason(CommandSourceStack source, Collection<? extends Entity> targets, CallbackInfoReturnable<Integer> cir) {
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity) {
                ((LivingEntityBridge) entity).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.COMMAND);
            }
        }
    }

    @Inject(method = "clearEffect", at = @At("HEAD"))
    private static void arclight$removeReason(CommandSourceStack source, Collection<? extends Entity> targets, MobEffect effect, CallbackInfoReturnable<Integer> cir) {
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity) {
                ((LivingEntityBridge) entity).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.COMMAND);
            }
        }
    }
}
