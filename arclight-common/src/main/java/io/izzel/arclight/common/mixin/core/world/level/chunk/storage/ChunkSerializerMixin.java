package io.izzel.arclight.common.mixin.core.world.level.chunk.storage;

import io.izzel.arclight.common.bridge.core.world.chunk.ChunkAccessBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.bukkit.craftbukkit.v.persistence.CraftPersistentDataContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {

    @Redirect(method = "read", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;setLightCorrect(Z)V"))
    private static void arclight$loadPersistent(ChunkAccess instance, boolean correct, ServerLevel level, PoiManager poiManager, ChunkPos pos, CompoundTag tag) {
        net.minecraft.nbt.Tag persistentBase = tag.get("ChunkBukkitValues");
        if (persistentBase instanceof CompoundTag) {
            ((CraftPersistentDataContainer) ((ChunkAccessBridge) instance).bridge$getPersistentDataContainer()).putAll((CompoundTag) persistentBase);
        }
        instance.setLightCorrect(correct);
    }


    @Inject(method = "write", at = @At("RETURN"))
    private static void arclight$savePersistent(ServerLevel level, ChunkAccess chunkAccess, CallbackInfoReturnable<CompoundTag> cir) {
        var container = (CraftPersistentDataContainer) ((ChunkAccessBridge) chunkAccess).bridge$getPersistentDataContainer();
        if (!container.isEmpty()) {
            cir.getReturnValue().put("ChunkBukkitValues", container.toTagCompound());
        }
    }
}
