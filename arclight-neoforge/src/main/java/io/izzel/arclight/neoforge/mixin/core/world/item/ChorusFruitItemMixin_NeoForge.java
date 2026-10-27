package io.izzel.arclight.neoforge.mixin.core.world.item;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TeleportRandomlyConsumeEffect.class)
public class ChorusFruitItemMixin_NeoForge {
    @Decorate(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;randomTeleport(DDDZLnet/minecraft/world/item/ItemStack;)Z"))
    private boolean arclight$randomTeleport(LivingEntity user, double x, double y, double z, boolean showParticles,
                                             ItemStack consumedStack) throws Throwable {
        var previous = ArclightCaptures.chorusPending;
        var chorus = com.mojang.datafixers.util.Pair.of(user, new org.apache.commons.lang3.mutable.MutableBoolean());
        ArclightCaptures.chorusPending = chorus;
        boolean result;
        try {
            result = (boolean) DecorationOps.callsite().invoke(user, x, y, z, showParticles, consumedStack);
        } finally {
            ArclightCaptures.chorusPending = previous;
        }
        // Retain the existing Bukkit stop contract; native Neo rejection is still ordinary false/retry.
        if (chorus.getSecond().booleanValue()) {
            return (boolean) DecorationOps.cancel().invoke(false);
        }
        return result;
    }
}
