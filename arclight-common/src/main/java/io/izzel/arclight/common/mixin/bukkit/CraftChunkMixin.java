package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.core.world.level.entity.PersistentEntitySectionManagerBridge;
import io.izzel.arclight.common.mod.ArclightConstants;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.bukkit.craftbukkit.v.CraftChunk;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

@Mixin(value = CraftChunk.class, remap = false)
public abstract class CraftChunkMixin {
    @Shadow
    public abstract CraftWorld getCraftWorld();

    @Shadow
    @Final
    private int x;

    @Shadow
    @Final
    private int z;

    /**
     * @author InitAuther97
     * @reason mimic Paper chunk system behavior: don't return entities for unloaded or invalid entities
     *         Here we do not return entities if entities are not yet loaded. So if load event is fired,
     *         unload event will be fired; If load event is not fired, then neither is unload event.
     */
    @Overwrite
    public Entity[] getEntities() {
        PersistentEntitySectionManager<net.minecraft.world.entity.Entity> entityManager = this.getCraftWorld().getHandle().entityManager;
        long pair = ChunkPos.asLong(this.x, this.z);
        if (!entityManager.areEntitiesLoaded(pair)) {
            return ArclightConstants.EMPTY_ENTITIES;
        }
        final var bridge = ((PersistentEntitySectionManagerBridge) entityManager);
        final var sectionStorage = (EntitySectionStorage<net.minecraft.world.entity.Entity>) bridge.getSectionStorage();
        return sectionStorage.getExistingSectionsInChunk(ChunkPos.asLong(this.x, this.z))
                .flatMap(EntitySection::getEntities)
                .map(net.minecraft.world.entity.Entity::bridge$getBukkitEntity)
                .filter(Objects::nonNull)
                .toArray(Entity[]::new);
    }
}
