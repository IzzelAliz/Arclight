package io.izzel.arclight.impl.mixin.optimization.general.trackingrange;

import net.minecraft.entity.Entity;
import net.minecraft.world.server.ChunkManager;
import org.spigotmc.TrackingRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChunkManager.class)
public class ChunkManagerMixin_TrackingRange {

    @ModifyVariable(method = "track", index = 3, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;getUpdateFrequency()I"))
    private int trackingRange$updateRange(int defaultRange, Entity entity) {
        return TrackingRange.getEntityTrackingRange(entity, defaultRange);
    }
}
