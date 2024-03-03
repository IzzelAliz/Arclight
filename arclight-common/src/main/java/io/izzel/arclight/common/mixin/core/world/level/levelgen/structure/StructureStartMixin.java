package io.izzel.arclight.common.mixin.core.world.level.levelgen.structure;

import io.izzel.arclight.common.bridge.core.world.level.levelgen.StructureStartBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.bukkit.craftbukkit.v.persistence.CraftPersistentDataContainer;
import org.bukkit.craftbukkit.v.persistence.CraftPersistentDataTypeRegistry;
import org.bukkit.craftbukkit.v.persistence.DirtyCraftPersistentDataContainer;
import org.bukkit.craftbukkit.v.util.CraftStructureTransformer;
import org.bukkit.craftbukkit.v.util.TransformerGeneratorAccess;
import org.bukkit.event.world.AsyncStructureGenerateEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(StructureStart.class)
public class StructureStartMixin implements StructureStartBridge {

    @Shadow @Final private PiecesContainer pieceContainer;
    @Shadow @Final private Structure structure;

    @Unique private AsyncStructureGenerateEvent.Cause arclight$cause = AsyncStructureGenerateEvent.Cause.WORLD_GENERATION;

    @Override
    public void bridge$setGenerateCause(AsyncStructureGenerateEvent.Cause cause) {
        this.arclight$cause = cause;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void placeInChunk(WorldGenLevel p_226851_, StructureManager p_226852_, ChunkGenerator p_226853_, RandomSource p_226854_, BoundingBox p_226855_, ChunkPos p_226856_) {
        List<StructurePiece> list = this.pieceContainer.pieces();
        if (!list.isEmpty()) {
            BoundingBox boundingbox = (list.get(0)).getBoundingBox();
            BlockPos blockpos = boundingbox.getCenter();
            BlockPos blockpos1 = new BlockPos(blockpos.getX(), boundingbox.minY(), blockpos.getZ());

            //for (StructurePiece structurepiece : list) {
            //    if (structurepiece.getBoundingBox().intersects(p_226855_)) {
            //        structurepiece.postProcess(p_226851_, p_226852_, p_226853_, p_226854_, p_226855_, p_226856_, blockpos1);
            //    }
            //}

            List<StructurePiece> pieces = list.stream().filter(piece -> piece.getBoundingBox().intersects(p_226855_)).toList();
            if (!pieces.isEmpty()) {
                var transformerAccess = new TransformerGeneratorAccess();
                transformerAccess.setHandle(p_226851_);
                transformerAccess.setStructureTransformer(new CraftStructureTransformer(arclight$cause, p_226851_, p_226852_, structure, p_226855_, p_226856_));
                for (StructurePiece piece : pieces) {
                    piece.postProcess(transformerAccess, p_226852_, p_226853_, p_226854_, p_226855_, p_226856_, blockpos1);
                }
                transformerAccess.getStructureTransformer().discard();
            }

            this.structure.afterPlace(p_226851_, p_226852_, p_226853_, p_226854_, p_226855_, p_226856_, this.pieceContainer);
        }
    }

    private static final CraftPersistentDataTypeRegistry DATA_TYPE_REGISTRY = new CraftPersistentDataTypeRegistry();
    public DirtyCraftPersistentDataContainer persistentDataContainer = new DirtyCraftPersistentDataContainer(DATA_TYPE_REGISTRY);

    @Override
    public CraftPersistentDataContainer bridge$getPersistentDataContainer() {
        return persistentDataContainer;
    }

    @Inject(method = "createTag", at = @At("RETURN"))
    private void arclight$writeBukkitContainer(StructurePieceSerializationContext structurePieceSerializationContext, ChunkPos chunkPos, CallbackInfoReturnable<CompoundTag> cir) {
        var tag = cir.getReturnValue();
        if (tag != null) {
            if (!persistentDataContainer.isEmpty()) {
                tag.put("StructureBukkitValues", persistentDataContainer.toTagCompound());
            }
        }
    }

    @Inject(method = "loadStaticStart", at = @At("RETURN"))
    private static void arclight$readBukkitContainer(StructurePieceSerializationContext structurePieceSerializationContext, CompoundTag compoundTag, long l, CallbackInfoReturnable<StructureStart> cir) {
        var structureStart = cir.getReturnValue();
        if (structureStart != null) {
            var nbt = compoundTag.get("StructureBukkitValues");
            if (nbt instanceof CompoundTag tag) {
                ((StructureStartBridge) (Object) structureStart).bridge$getPersistentDataContainer().putAll(tag);
            }
        }
    }
}
