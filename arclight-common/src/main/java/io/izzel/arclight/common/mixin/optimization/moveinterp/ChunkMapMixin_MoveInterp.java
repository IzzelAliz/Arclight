package io.izzel.arclight.common.mixin.optimization.moveinterp;

import io.izzel.arclight.common.bridge.core.world.server.ChunkMap_TrackedEntityBridge;
import io.izzel.arclight.common.mod.util.optimization.moveinterp.MoveInterpolatorService;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public class ChunkMapMixin_MoveInterp {

    @Shadow @Final public Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;

    @Inject(method = "addEntity", at = @At(value = "INVOKE", remap = false, shift = At.Shift.AFTER, target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;put(ILjava/lang/Object;)Ljava/lang/Object;"))
    private void arclight$addEntity(Entity entity, CallbackInfo ci) {
        if (!(entity instanceof ServerPlayer)) {
            var trackedEntity = this.entityMap.get(entity.getId());
            MoveInterpolatorService.getInterpolator().add(((ChunkMap_TrackedEntityBridge) trackedEntity).bridge$getServerEntity());
        }
    }

    @Inject(method = "removeEntity", at = @At(value = "INVOKE", remap = false, target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;remove(I)Ljava/lang/Object;"))
    private void arclight$removeEntity(Entity entity, CallbackInfo ci) {
        var trackedEntity = this.entityMap.get(entity.getId());
        if (trackedEntity != null) {
            MoveInterpolatorService.getInterpolator().remove(((ChunkMap_TrackedEntityBridge) trackedEntity).bridge$getServerEntity());
        }
    }

    @Inject(method = "tick()V", at = @At("RETURN"))
    private void arclight$mainTick(CallbackInfo ci) {
        MoveInterpolatorService.getInterpolator().mainThreadTick();
    }
}
