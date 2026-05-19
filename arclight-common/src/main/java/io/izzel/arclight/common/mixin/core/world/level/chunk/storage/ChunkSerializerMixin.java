package io.izzel.arclight.common.mixin.core.world.level.chunk.storage;

import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SerializableChunkData.class)
public class ChunkSerializerMixin {

    // TODO PalettedContainerRO is always PalettedContainer, which is RW

    // unpackStructureStart CraftBukkit part implemented in StructureStart#loadStaticStart
}
