package io.izzel.arclight.impl.mixin.optimization.dfu;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TileEntityType.class)
public class TileEntityTypeMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private static <T extends TileEntity> TileEntityType<T> register(String key, TileEntityType.Builder<T> builder) {
        return Registry.register(Registry.BLOCK_ENTITY_TYPE, key, builder.build(null));
    }
}
