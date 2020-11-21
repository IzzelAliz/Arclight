package io.izzel.arclight.common.mod.server.event;

import io.izzel.arclight.common.bridge.world.chunk.ChunkBridge;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.bukkit.craftbukkit.v.persistence.CraftPersistentDataContainer;

public class ChunkEventHandler {

    @SubscribeEvent
    public void onChunkLoad(ChunkDataEvent.Load event) {
        if (event.getStatus() == ChunkStatus.Type.LEVELCHUNK) {
            IChunk chunk = event.getChunk();
            CompoundNBT nbt = event.getData();
            INBT values = nbt.get("ChunkBukkitValues");
            if (values instanceof CompoundNBT) {
                ((ChunkBridge) chunk).bridge$getPersistentContainer().putAll((CompoundNBT) values);
            }
        }
    }

    @SubscribeEvent
    public void onChunkSave(ChunkDataEvent.Save event) {
        IChunk chunk = event.getChunk();
        if (chunk instanceof ChunkBridge) {
            CraftPersistentDataContainer container = ((ChunkBridge) chunk).bridge$getPersistentContainer();
            if (!container.isEmpty()) {
                event.getData().put("ChunkBukkitValues", container.toTagCompound());
            }
        }
    }
}
