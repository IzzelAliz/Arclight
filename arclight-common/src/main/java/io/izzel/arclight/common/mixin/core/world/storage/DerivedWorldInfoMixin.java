package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.world.storage.DerivedWorldInfoBridge;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.DimensionType;
import net.minecraft.world.storage.DerivedWorldInfo;
import net.minecraft.world.storage.IServerWorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DerivedWorldInfo.class)
public class DerivedWorldInfoMixin implements DerivedWorldInfoBridge {

    @Shadow @Final private IServerWorldInfo delegate;

    private RegistryKey<DimensionType> typeKey;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public String getWorldName() {
        if (typeKey == null || typeKey == DimensionType.OVERWORLD) {
            return this.delegate.getWorldName();
        } else {
            String worldName = this.delegate.getWorldName() + "/";
            String suffix;
            if (typeKey == DimensionType.THE_END) {
                suffix = "DIM1";
            } else if (typeKey == DimensionType.THE_NETHER) {
                suffix = "DIM-1";
            } else {
                suffix = typeKey.getLocation().getNamespace() + "/" + typeKey.getLocation().getPath();
            }
            return worldName + suffix;
        }
    }

    @Override
    public IServerWorldInfo bridge$getDelegate() {
        return delegate;
    }

    @Override
    public void bridge$setDimType(RegistryKey<DimensionType> typeKey) {
        this.typeKey = typeKey;
    }
}
