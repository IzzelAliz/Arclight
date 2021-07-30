package io.izzel.arclight.common.mod.server.event;

import io.izzel.arclight.common.bridge.core.world.chunk.ChunkBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.bukkit.craftbukkit.v.persistence.CraftPersistentDataContainer;

public class ChunkEventHandler {

    @SubscribeEvent
    public void onChunkLoad(ChunkDataEvent.Load event) {
        if (event.getStatus() == ChunkStatus.ChunkType.LEVELCHUNK) {
            ChunkAccess chunk = event.getChunk();
            CompoundTag nbt = event.getData();
            Tag values = nbt.get("ChunkBukkitValues");
            if (values instanceof CompoundTag) {
                ((ChunkBridge) chunk).bridge$getPersistentContainer().putAll((CompoundTag) values);
            }
        }
    }

    @SubscribeEvent
    public void onChunkSave(ChunkDataEvent.Save event) {
        ChunkAccess chunk = event.getChunk();
        if (chunk instanceof ChunkBridge) {
            CraftPersistentDataContainer container = ((ChunkBridge) chunk).bridge$getPersistentContainer();
            if (!container.isEmpty()) {
                event.getData().put("ChunkBukkitValues", container.toTagCompound());
            }
        }
    }
}
