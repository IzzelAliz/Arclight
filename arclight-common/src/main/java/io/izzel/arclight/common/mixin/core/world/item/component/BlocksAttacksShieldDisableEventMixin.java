package io.izzel.arclight.common.mixin.core.world.item.component;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.player.PlayerShieldDisableEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlocksAttacks.class)
public abstract class BlocksAttacksShieldDisableEventMixin {

    // This point is after native cooldown > 0 and Player checks. Cancellation exits
    // disable itself, so neither the item-cooldown event nor stop/sound tails run.
    @Decorate(method = "disable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/item/ItemStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemCooldowns;addCooldown(Lnet/minecraft/world/item/ItemStack;I)V"),
        require = 1, allow = 1)
    private void arclight$playerShieldDisable(ItemCooldowns cooldowns, ItemStack blockingWith, int ticks,
                                             @Local(ordinal = 0) ServerLevel level,
                                             @Local(ordinal = 0) LivingEntity user) throws Throwable {
        var capture = ArclightCaptures.shieldDisable;
        LivingEntity attacker = null;
        if (capture != null && capture.getLeft() == (Object) this && capture.getMiddle() == blockingWith
            && capture.getRight().getFirst() == user) {
            attacker = capture.getRight().getSecond();
            ArclightCaptures.shieldDisable = null;
        }
        if (attacker != null) {
            PlayerShieldDisableEvent event = new PlayerShieldDisableEvent(
                (org.bukkit.entity.Player) ((LivingEntityBridge) user).bridge$getBukkitEntity(),
                ((EntityBridge) attacker).bridge$getBukkitEntity(), ticks);
            if (!event.callEvent()) {
                DecorationOps.cancel().invoke();
                return;
            }
            ticks = event.getCooldown();
        }
        // Forward the edited ticks even when zero; Paper still executes stop/sound.
        DecorationOps.callsite().invoke(cooldowns, blockingWith, ticks);
    }
}
