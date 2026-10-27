package io.izzel.arclight.common.mixin.core.world.entity.player;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerCombatPaperEventMixin {

    @Decorate(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;cannotAttack(Lnet/minecraft/world/entity/Entity;)Z"), require = 1, allow = 1)
    private boolean arclight$prePlayerAttack(Player player, Entity target) throws Throwable {
        // On NeoForge this remains after onPlayerAttackTarget's acceptance branch.
        boolean cannotAttack = (boolean) DecorationOps.callsite().invoke(player, target);
        return arclight$paperCannotAttack(player, target, cannotAttack);
    }

    @Decorate(method = "stabAttack(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/Entity;FZZZ)Z",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;cannotAttack(Lnet/minecraft/world/entity/Entity;)Z"), require = 1, allow = 1)
    private boolean arclight$prePlayerStabAttack(Player player, Entity target) throws Throwable {
        boolean cannotAttack = (boolean) DecorationOps.callsite().invoke(player, target);
        return arclight$paperCannotAttack(player, target, cannotAttack);
    }

    @Unique
    private boolean arclight$paperCannotAttack(Player player, Entity target, boolean cannotAttack) {
        if (player.level().isClientSide()) {
            return cannotAttack;
        }
        PrePlayerAttackEntityEvent event = new PrePlayerAttackEntityEvent(
            (org.bukkit.entity.Player) ((PlayerBridge) player).bridge$getBukkitEntity(),
            ((EntityBridge) target).bridge$getBukkitEntity(), !cannotAttack);
        // Dispatch even for an unattackable target; its API makes that veto immutable.
        boolean allowed = event.callEvent();
        return cannotAttack || !allowed;
    }
}
