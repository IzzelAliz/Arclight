package io.izzel.arclight.common.mixin.core.world.entity.npc;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.Level;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WanderingTrader.class)
public abstract class WanderingTraderMixin extends AbstractVillagerMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"), require = 1)
    private void arclight$defaultDespawnDelay(EntityType<? extends WanderingTrader> type, Level level, CallbackInfo ci) {
        ((WanderingTrader) (Object) this).setDespawnDelay(48000);
    }

    @Inject(method = "maybeDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/wanderingtrader/WanderingTrader;discard()V"))
    private void arclight$despawn(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }
}
