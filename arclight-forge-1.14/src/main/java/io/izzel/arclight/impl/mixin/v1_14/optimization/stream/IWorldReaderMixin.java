package io.izzel.arclight.impl.mixin.v1_14.optimization.stream;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IWorldReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

@Mixin(IWorldReader.class)
public interface IWorldReaderMixin {

    // @formatter:off
    @Shadow Stream<VoxelShape> getCollisionShapes(@Nullable Entity entityIn, AxisAlignedBB aabb, Set<Entity> entitiesToIgnore);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    default boolean isCollisionBoxesEmpty(@Nullable Entity entityIn, AxisAlignedBB aabb, Set<Entity> entitiesToIgnore) {
        Iterator<VoxelShape> iterator = this.getCollisionShapes(entityIn, aabb, entitiesToIgnore).iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().isEmpty()) return false;
        }
        return true;
    }
}
