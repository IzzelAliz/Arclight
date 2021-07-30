package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.core.world.storage.DerivedWorldInfoBridge;
import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DerivedLevelData.class)
public class DerivedWorldInfoMixin implements DerivedWorldInfoBridge {

    @Shadow @Final private ServerLevelData wrapped;

    private ResourceKey<DimensionType> typeKey;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public String getLevelName() {
        if (typeKey == null || typeKey == DimensionType.OVERWORLD_LOCATION) {
            return this.wrapped.getLevelName();
        } else {
            if (ArclightConfig.spec().getCompat().isSymlinkWorld()) {
                String worldName = this.wrapped.getLevelName() + "_";
                String suffix;
                if (typeKey == DimensionType.END_LOCATION) {
                    suffix = "nether";
                } else if (typeKey == DimensionType.NETHER_LOCATION) {
                    suffix = "the_end";
                } else {
                    suffix = (typeKey.location().getNamespace() + "/" + typeKey.location().getPath()).replace('/', '_');
                }
                return worldName + suffix;
            } else {
                String worldName = this.wrapped.getLevelName() + "/";
                String suffix;
                if (typeKey == DimensionType.END_LOCATION) {
                    suffix = "DIM1";
                } else if (typeKey == DimensionType.NETHER_LOCATION) {
                    suffix = "DIM-1";
                } else {
                    suffix = typeKey.location().getNamespace() + "/" + typeKey.location().getPath();
                }
                return worldName + suffix;
            }
        }
    }

    @Override
    public ServerLevelData bridge$getDelegate() {
        return wrapped;
    }

    @Override
    public void bridge$setDimType(ResourceKey<DimensionType> typeKey) {
        this.typeKey = typeKey;
    }
}
