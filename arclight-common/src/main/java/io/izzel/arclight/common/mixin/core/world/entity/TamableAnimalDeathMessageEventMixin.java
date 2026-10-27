package io.izzel.arclight.common.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.event.entity.TameableDeathMessageEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import org.bukkit.entity.Tameable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TamableAnimal.class)
public abstract class TamableAnimalDeathMessageEventMixin {

    @Decorate(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;sendSystemMessage(Lnet/minecraft/network/chat/Component;)V"),
        require = 1, allow = 1)
    private void arclight$tameableDeathMessage(ServerPlayer owner, Component message) throws Throwable {
        var event = new TameableDeathMessageEvent(
            (Tameable) ((EntityBridge) this).bridge$getBukkitEntity(), PaperAdventure.asAdventure(message));
        if (event.callEvent()) {
            DecorationOps.callsite().invoke(owner, PaperAdventure.asVanilla(event.deathMessage()));
        }
        // Cancellation suppresses only the message, never the native death continuation.
    }
}
