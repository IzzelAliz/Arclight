package io.izzel.arclight.common.mixin.core.command.impl;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import net.minecraft.command.CommandSource;
import net.minecraft.command.impl.EffectCommand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(EffectCommand.class)
public class EffectCommandMixin {

    @Inject(method = "addEffect", at = @At("HEAD"))
    private static void arclight$addReason(CommandSource source, Collection<? extends Entity> targets, Effect effect, Integer seconds, int amplifier, boolean showParticles, CallbackInfoReturnable<Integer> cir) {
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity) {
                ((LivingEntityBridge) entity).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.COMMAND);
            }
        }
    }

    @Inject(method = "clearAllEffects", at = @At("HEAD"))
    private static void arclight$removeAllReason(CommandSource source, Collection<? extends Entity> targets, CallbackInfoReturnable<Integer> cir) {
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity) {
                ((LivingEntityBridge) entity).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.COMMAND);
            }
        }
    }

    @Inject(method = "clearEffect", at = @At("HEAD"))
    private static void arclight$removeReason(CommandSource source, Collection<? extends Entity> targets, Effect effect, CallbackInfoReturnable<Integer> cir) {
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity) {
                ((LivingEntityBridge) entity).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.COMMAND);
            }
        }
    }
}
