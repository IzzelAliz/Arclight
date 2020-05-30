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

@Mixin(StatisticsManager.class)
public abstract class StatisticsManagerMixin {

    // @formatter:off
    @Shadow public abstract int getValue(Stat<?> stat);
    // @formatter:on

    @Inject(method = "increment", cancellable = true, at = @At("HEAD"))
    public void arclight$statsIncl(PlayerEntity player, Stat<?> stat, int amount, CallbackInfo ci) {
        Cancellable cancellable = CraftEventFactory.handleStatisticsIncrease(player, stat, this.getValue(stat), amount);
        if (cancellable != null && cancellable.isCancelled()) {
            ci.cancel();
        }
    }
}
