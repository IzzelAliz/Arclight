package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.world.storage.DerivedWorldInfoBridge;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.storage.DerivedWorldInfo;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DerivedWorldInfo.class)
public class DerivedWorldInfoMixin implements DerivedWorldInfoBridge {

    // @formatter:off
    @Shadow @Final private WorldInfo delegate;
    // @formatter:on

    private DimensionType type;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public String getWorldName() {
        if (type == null || type.getId() == 0 || type.directory.isEmpty()) {
            return this.delegate.getWorldName();
        } else {
            return this.delegate.getWorldName() + "/" + type.directory;
        }
    }

    @Override
    public void bridge$setDimension(DimensionType dimensionType) {
        this.type = dimensionType;
    }
}
