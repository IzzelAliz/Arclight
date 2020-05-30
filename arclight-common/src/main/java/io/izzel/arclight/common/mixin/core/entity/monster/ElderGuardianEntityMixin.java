package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.mixin.core.entity.CreatureEntityMixin;
import net.minecraft.entity.monster.ElderGuardianEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ElderGuardianEntity.class)
public abstract class ElderGuardianEntityMixin extends CreatureEntityMixin {

    @Redirect(method = "updateAITasks", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private boolean arclight$potionReason(ServerPlayerEntity playerEntity, EffectInstance effectInstanceIn) {
        ((ServerPlayerEntityBridge) playerEntity).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
        return playerEntity.addPotionEffect(effectInstanceIn);
    }
}
