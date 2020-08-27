package io.izzel.arclight.common.mixin.core.stats;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatisticsManager;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.Cancellable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(StatisticsManager.class)
public abstract class StatisticsManagerMixin {

    // @formatter:off
    @Shadow public abstract int getValue(Stat<?> stat);
    // @formatter:on

    @Inject(method = "increment", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/stats/StatisticsManager;setValue(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/stats/Stat;I)V"))
    public void arclight$statsIncl(PlayerEntity player, Stat<?> stat, int amount, CallbackInfo ci, int i) {
        Cancellable cancellable = CraftEventFactory.handleStatisticsIncrease(player, stat, this.getValue(stat), i);
        if (cancellable != null && cancellable.isCancelled()) {
            ci.cancel();
        }
    }
}
