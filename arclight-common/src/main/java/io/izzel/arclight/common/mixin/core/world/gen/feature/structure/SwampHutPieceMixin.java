package io.izzel.arclight.common.mixin.core.world.gen.feature.structure;

import io.izzel.arclight.common.bridge.core.world.IWorldWriterBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.SwamplandHutPiece;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(SwamplandHutPiece.class)
public class SwampHutPieceMixin {

    @Inject(method = "postProcess", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$spawnReason1(WorldGenLevel level, StructureFeatureManager p_192667_, ChunkGenerator p_192668_, Random p_192669_, BoundingBox p_192670_, ChunkPos p_192671_, BlockPos p_192672_, CallbackInfo ci) {
        ((IWorldWriterBridge) level).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.CHUNK_GEN);
    }

    @Inject(method = "spawnCat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$spawnReason2(ServerLevelAccessor worldIn, BoundingBox p_214821_2_, CallbackInfo ci) {
        ((IWorldWriterBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.CHUNK_GEN);
    }
}
