package io.izzel.arclight.impl.mixin.v1_14.core.world.gen.feature.structure;

import io.izzel.arclight.common.bridge.world.IWorldWriterBridge;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.feature.structure.SwampHutPiece;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(SwampHutPiece.class)
public class SwampHutPieceMixin_1_14 {

    @Inject(method = "addComponentParts", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IWorld;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    public void arclight$spawnReason(IWorld worldIn, Random randomIn, MutableBoundingBox structureBoundingBoxIn, ChunkPos chunkPosIn, CallbackInfoReturnable<Boolean> cir) {
        ((IWorldWriterBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.CHUNK_GEN);
    }
}
