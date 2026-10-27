package io.izzel.arclight.common.mixin.core.world.entity.player;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerShieldDisableEventMixin {

    // Scope only the original component call, after the native super.blockUsingItem
    // callback. Do not replace the attack, disable calculation or loader callbacks.
    @Decorate(method = "blockUsingItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/BlocksAttacks;disable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/item/ItemStack;)V"),
        require = 1, allow = 1)
    private void arclight$shieldDisableAttacker(BlocksAttacks component, ServerLevel level, LivingEntity user,
                                              float seconds, ItemStack blockingWith,
                                              @Local(ordinal = 0) LivingEntity attacker) throws Throwable {
        var previous = ArclightCaptures.shieldDisable;
        ArclightCaptures.shieldDisable = org.apache.commons.lang3.tuple.Triple.of(component, blockingWith,
            com.mojang.datafixers.util.Pair.of(user, attacker));
        try {
            DecorationOps.callsite().invoke(component, level, user, seconds, blockingWith);
        } finally {
            ArclightCaptures.shieldDisable = previous;
        }
    }
}
