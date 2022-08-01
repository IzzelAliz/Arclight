package io.izzel.arclight.common.mixin.core.world.effect;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(MobEffectUtil.class)
public class MobEffectUtilMixin {

    @Inject(method = "addEffectToPlayersAround", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", remap = false, target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"))
    private static void arclight$pushCause(ServerLevel p_216947_, Entity p_216948_, Vec3 p_216949_, double p_216950_, MobEffectInstance p_216951_, int p_216952_, CallbackInfoReturnable<List<ServerPlayer>> cir,
                                           MobEffect effect, List<ServerPlayer> players) {
        var cause = ArclightCaptures.getEffectCause();
        if (cause != null) {
            for (ServerPlayer player : players) {
                ((ServerPlayerEntityBridge) player).bridge$pushEffectCause(cause);
            }
        }
    }
}
