package io.izzel.arclight.common.mixin.core.advancements;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.izzel.arclight.common.bridge.advancement.AdvancementBridge;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {

    @Shadow private ServerPlayerEntity player;

    @Inject(method = "grantCriterion",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/Advancement;getRewards()Lnet/minecraft/advancements/AdvancementRewards;"))
    public void arclight$callEvent(Advancement advancementIn, String criterionKey, CallbackInfoReturnable<Boolean> cir) {
        Bukkit.getPluginManager().callEvent(new org.bukkit.event.player.PlayerAdvancementDoneEvent((Player) ((EntityBridge) this.player).bridge$getBukkitEntity(), ((AdvancementBridge) advancementIn).bridge$getBukkit()));
    }
}
