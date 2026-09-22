package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.mod.util.ChorusTeleportContext;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TeleportRandomlyConsumeEffect.class)
public class ChorusFruitItemMixin {
    @Decorate(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;randomTeleport(DDDZ)Z"))
    private boolean arclight$randomTeleport(LivingEntity user, double x, double y, double z, boolean showParticles) throws Throwable {
        ChorusTeleportContext.Scope scope = ChorusTeleportContext.push(user);
        boolean result;
        try {
            result = (boolean) DecorationOps.callsite().invoke(user, x, y, z, showParticles);
        } finally {
            ChorusTeleportContext.pop(scope);
        }
        // Cancel after releasing the scope: Decorate early-return is bytecode control flow.
        if (scope.cancelled()) {
            return (boolean) DecorationOps.cancel().invoke(false);
        }
        return result;
    }
}
