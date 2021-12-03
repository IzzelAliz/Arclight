package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.core.world.storage.DerivedWorldInfoBridge;
import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DerivedLevelData.class)
public class DerivedWorldInfoMixin implements DerivedWorldInfoBridge {

    @Shadow @Final private ServerLevelData wrapped;

    private ResourceKey<LevelStem> typeKey;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public String getLevelName() {
        if (typeKey == null || typeKey == LevelStem.OVERWORLD) {
            return this.wrapped.getLevelName();
        } else {
            if (ArclightConfig.spec().getCompat().isSymlinkWorld()) {
                String worldName = this.wrapped.getLevelName() + "_";
                String suffix;
                if (typeKey == LevelStem.NETHER) {
                    suffix = "nether";
                } else if (typeKey == LevelStem.END) {
                    suffix = "the_end";
                } else {
                    suffix = (typeKey.location().getNamespace() + "_" + typeKey.location().getPath()).replace('/', '_');
                }
                return worldName + suffix;
            } else {
                String worldName = this.wrapped.getLevelName() + "/";
                String suffix;
                if (typeKey == LevelStem.END) {
                    suffix = "DIM1";
                } else if (typeKey == LevelStem.NETHER) {
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
    public void bridge$setDimType(ResourceKey<LevelStem> typeKey) {
        this.typeKey = typeKey;
    }
}
