package io.izzel.arclight.common.mixin.core.server;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import io.izzel.arclight.common.bridge.core.advancements.AdvancementHolderBridge;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {

    @Shadow private ServerPlayer player;

    @Decorate(method = "award(Lnet/minecraft/advancements/AdvancementHolder;Ljava/lang/String;)Z", require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementProgress;grantProgress(Ljava/lang/String;)Z"))
    private boolean arclight$criterionGrant(AdvancementProgress progress, String criterion,
                                          @Local(ordinal = 0) AdvancementHolder holder) throws Throwable {
        boolean granted = (boolean) DecorationOps.callsite().invoke(progress, criterion);
        if (granted && !new PlayerAdvancementCriterionGrantEvent((Player) this.player.bridge$getBukkitEntity(),
            ((AdvancementHolderBridge) (Object) holder).bridge$getBukkit(), criterion).callEvent()) {
            // Match Paper: undo the raw grant before listener removal, dirty tracking and completion side effects.
            progress.revokeProgress(criterion);
            return (boolean) DecorationOps.cancel().invoke(false);
        }
        return granted;
    }

    @Inject(method = "award",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementRewards;grant(Lnet/minecraft/server/level/ServerPlayer;)V"))
    public void arclight$callEvent(AdvancementHolder advancementHolder, String criterionKey, CallbackInfoReturnable<Boolean> cir) {
        Bukkit.getPluginManager().callEvent(new org.bukkit.event.player.PlayerAdvancementDoneEvent((Player) this.player.bridge$getBukkitEntity(), ((AdvancementHolderBridge) (Object) advancementHolder).bridge$getBukkit()));
    }
}
