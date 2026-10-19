package io.izzel.arclight.common.mixin.core.world.level.chunk.storage;

import com.llamalad7.mixinextras.sugar.Local;
import io.izzel.arclight.common.bridge.core.world.chunk.ChunkAccessBridge;
import io.izzel.arclight.common.bridge.core.world.level.chunk.storage.SerializableChunkDataBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SerializableChunkData.class)
public class ChunkSerializerMixin implements SerializableChunkDataBridge {

    @Unique
    private CompoundTag arclight$persistentData;

    @Override
    public void bridge$setPersistentData(CompoundTag persistentData) {
        this.arclight$persistentData = persistentData;
    }

    @Inject(method = "parse", at = @At("RETURN"))
    private static void arclight$parsePdc(LevelHeightAccessor levelHeight, PalettedContainerFactory containerFactory, CompoundTag chunkData, CallbackInfoReturnable<SerializableChunkData> cir) {
        if (cir.getReturnValue() != null) {
            ((SerializableChunkDataBridge) (Object) cir.getReturnValue()).bridge$setPersistentData(chunkData.getCompound("ChunkBukkitValues").map(CompoundTag::copy).orElse(null));
        }
    }

    // Load onto the actual ChunkAccess before the full-chunk ImposterProtoChunk wrapper
    // is returned. This also covers proto chunks undergoing old-world upgrading.
    @Inject(method = "read", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;setLightCorrect(Z)V"))
    private void arclight$readPdc(CallbackInfoReturnable<ProtoChunk> cir, @Local ChunkAccess chunk) {
        if (this.arclight$persistentData != null) {
            ((CraftPersistentDataContainer) ((ChunkAccessBridge) chunk).bridge$getPersistentDataContainer()).putAll(this.arclight$persistentData.copy());
        }
    }

    @Inject(method = "copyOf", at = @At("RETURN"))
    private static void arclight$copyPdc(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<SerializableChunkData> cir) {
        var container = (CraftPersistentDataContainer) ((ChunkAccessBridge) chunk).bridge$getPersistentDataContainer();
        // Snapshot even when the dirty flag was already consumed by tryMarkSaved.
        // Deep-copy nested tags before native write() runs on the background executor.
        ((SerializableChunkDataBridge) (Object) cir.getReturnValue()).bridge$setPersistentData(container.isEmpty() ? null : container.toTagCompound().copy());
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void arclight$writePdc(CallbackInfoReturnable<CompoundTag> cir) {
        if (this.arclight$persistentData != null) {
            cir.getReturnValue().put("ChunkBukkitValues", this.arclight$persistentData.copy());
        }
    }

    // TODO PalettedContainerRO is always PalettedContainer, which is RW

    // unpackStructureStart CraftBukkit part implemented in StructureStart#loadStaticStart
}
