package io.izzel.arclight.common.mixin.core.stats;

import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.Cancellable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(StatsCounter.class)
public abstract class StatisticsCounterMixin {

    // @formatter:off
    @Shadow public abstract int getValue(Stat<?> stat);
    // @formatter:on

    @Inject(method = "increment", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/stats/StatsCounter;setValue(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/stats/Stat;I)V"))
    public void arclight$statsIncl(Player player, Stat<?> stat, int amount, CallbackInfo ci, int i) {
        Cancellable cancellable = CraftEventFactory.handleStatisticsIncrease(player, stat, this.getValue(stat), i);
        if (cancellable != null && cancellable.isCancelled()) {
            ci.cancel();
        }
    }
}
