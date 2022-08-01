package io.izzel.arclight.common.mixin.core.world.entity.monster.warden;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Warden.class)
public class WardenMixin {

    @Inject(method = "applyDarknessAround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectUtil;addEffectToPlayersAround(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;DLnet/minecraft/world/effect/MobEffectInstance;I)Ljava/util/List;"))
    private static void arclight$reason(ServerLevel p_219376_, Vec3 p_219377_, Entity p_219378_, int p_219379_, CallbackInfo ci) {
        ArclightCaptures.captureEffectCause(EntityPotionEffectEvent.Cause.WARDEN);
    }
}
