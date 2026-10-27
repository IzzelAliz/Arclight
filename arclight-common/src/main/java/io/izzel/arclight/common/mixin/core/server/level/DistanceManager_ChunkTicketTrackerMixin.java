package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.server.level.DistanceManagerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;

@Mixin(targets = "net.minecraft.server.level.LoadingChunkTracker")
@io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod(modid = "moonrise", condition = io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod.ModCondition.ABSENT)
public class DistanceManager_ChunkTicketTrackerMixin {
    // @formatter:off
    @Shadow @Final private DistanceManager distanceManager;
    // @formatter:on

    @Decorate(method = "setLevel", at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"))
    private boolean arclight$setLevel(Set instance, Object e) throws Throwable {
        ((DistanceManagerBridge) distanceManager).arclight$offerUpdate((ChunkHolder) e);
        return (boolean) DecorationOps.callsite().invoke(instance, e);
    }
}
