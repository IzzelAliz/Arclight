package io.izzel.arclight.common.mixin.core.world.entity.player;

import com.destroystokyo.paper.event.player.PlayerAttackEntityCooldownResetEvent;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerAttackCooldownEventMixin {

    @Unique private Entity arclight$cooldownResetTarget;

    @Decorate(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;onAttack()V"), require = 1, allow = 1)
    private void arclight$attackCooldownTarget(Player player, Entity target) throws Throwable {
        Entity previousTarget = this.arclight$cooldownResetTarget;
        this.arclight$cooldownResetTarget = target;
        try {
            // Keep the native virtual call, including its superclass/mod tail.
            DecorationOps.callsite().invoke(player);
        } finally {
            this.arclight$cooldownResetTarget = previousTarget;
        }
    }

    @SuppressWarnings("removal")
    @Decorate(method = "onAttack()V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;resetOnlyAttackStrengthTicker()V"), require = 1, allow = 1)
    private void arclight$attackCooldownReset(Player player) throws Throwable {
        Entity target = this.arclight$cooldownResetTarget;
        // Claim before dispatch: an ordinary onAttack() from a listener has no target.
        this.arclight$cooldownResetTarget = null;
        if (target == null || player.level().isClientSide()
            || new PlayerAttackEntityCooldownResetEvent(
                (org.bukkit.entity.Player) ((PlayerBridge) player).bridge$getBukkitEntity(),
                ((EntityBridge) target).bridge$getBukkitEntity(), player.getAttackStrengthScale(0.0F)).callEvent()) {
            // A veto skips only this reset, not onAttack's superclass continuation.
            DecorationOps.callsite().invoke(player);
        }
    }
}
