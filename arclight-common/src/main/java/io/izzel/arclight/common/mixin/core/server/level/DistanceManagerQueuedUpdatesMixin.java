package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.server.level.DistanceManagerBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.mixin.Decorate;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DistanceManager.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class DistanceManagerQueuedUpdatesMixin {
    @Decorate(method = "runAllUpdates", inject = true, at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z"))
    private void arclight$runQueuedUpdates(ChunkMap map) {
        ((DistanceManagerBridge) this).bridge$runQueuedUpdates(map);
    }
}
