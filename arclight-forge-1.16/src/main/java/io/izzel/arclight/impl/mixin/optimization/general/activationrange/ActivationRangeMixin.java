package io.izzel.arclight.impl.mixin.optimization.general.activationrange;

import io.izzel.arclight.impl.bridge.EntityBridge_ActivationRange;
import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.chunk.Chunk;
import org.spigotmc.ActivationRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ActivationRange.class, remap = false)
public class ActivationRangeMixin {

    /**
     * @author IzzelAliz
     * @reason entityLists
     */
    @Overwrite
    private static void activateChunkEntities(Chunk chunk) {
        for (ClassInheritanceMultiMap<Entity> entityList : chunk.entityLists) {
            for (Entity entity : entityList) {
                ((EntityBridge_ActivationRange) entity).bridge$updateActivation();
            }
        }
    }
}
