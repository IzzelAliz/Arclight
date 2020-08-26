package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.mixin.core.entity.CreatureEntityMixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.AbstractRaiderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.raid.Raid;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(AbstractRaiderEntity.class)
public abstract class AbstractRaiderEntityMixin extends CreatureEntityMixin {

    @Inject(method = "onDeath", locals = LocalCapture.CAPTURE_FAILHARD, require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$raid(DamageSource cause, CallbackInfo ci, Entity entity, Raid raid, ItemStack itemStack, PlayerEntity playerEntity) {
        ((PlayerEntityBridge) playerEntity).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.PATROL_CAPTAIN);
    }
}
