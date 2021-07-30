package io.izzel.arclight.common.mixin.core.world.entity.raider;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Raider.class)
public abstract class RaiderMixin extends PathfinderMobMixin {

    @Inject(method = "die", locals = LocalCapture.CAPTURE_FAILHARD, require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private void arclight$raid(DamageSource cause, CallbackInfo ci, Entity entity, Raid raid, ItemStack itemStack, Player playerEntity) {
        ((PlayerEntityBridge) playerEntity).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.PATROL_CAPTAIN);
    }
}
