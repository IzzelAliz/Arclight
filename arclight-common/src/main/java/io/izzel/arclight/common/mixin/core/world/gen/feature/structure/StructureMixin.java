package io.izzel.arclight.common.mixin.core.world.gen.feature.structure;

import net.minecraft.world.IWorld;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.feature.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Structure.class)
public class StructureMixin {

    @Redirect(method = "getStarts", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/IWorld;getChunk(IILnet/minecraft/world/chunk/ChunkStatus;)Lnet/minecraft/world/chunk/IChunk;"))
    private IChunk arclight$notLoadChunk(IWorld iWorld, int chunkX, int chunkZ, ChunkStatus requiredStatus) {
        return iWorld.getChunk(chunkX, chunkZ, requiredStatus, false);
    }
}
