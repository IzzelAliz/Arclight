package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import net.minecraft.world.chunk.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.v.CraftChunk;
import org.bukkit.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CraftChunk.class, remap = false)
public abstract class CraftChunkMixin {

    // @formatter:off
    @Shadow public abstract boolean isLoaded();
    @Shadow public abstract World getWorld();
    @Shadow @Final private int x;
    @Shadow @Final private int z;
    @Shadow public abstract Chunk getHandle();
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public Entity[] getEntities() {
        if (!this.isLoaded()) {
            this.getWorld().getChunkAt(this.x, this.z);
        }
        int count = 0;
        int index = 0;
        net.minecraft.world.chunk.Chunk chunk = this.getHandle();
        for (int i = 0; i < 16; ++i) {
            count += chunk.entityLists[i].size();
        }
        Entity[] entities = new Entity[count];
        for (int j = 0; j < 16; ++j) {
            Object[] array;
            for (int length = (array = chunk.entityLists[j].toArray()).length, k = 0; k < length; ++k) {
                Object obj = array[k];
                if (obj instanceof net.minecraft.entity.Entity) {
                    entities[index++] = ((EntityBridge) obj).bridge$getBukkitEntity();
                }
            }
        }
        return entities;
    }
}
