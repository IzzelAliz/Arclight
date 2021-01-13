package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.world.storage.SaveFormatBridge;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.Dimension;
import net.minecraft.world.World;
import net.minecraft.world.storage.SaveFormat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.nio.file.Path;

@Mixin(SaveFormat.LevelSave.class)
public class SaveFormat_LevelSaveMixin implements SaveFormatBridge.LevelSaveBridge {

    @Shadow @Final public Path saveDir;

    private RegistryKey<Dimension> dimensionType;

    @Override
    public void bridge$setDimType(RegistryKey<Dimension> typeKey) {
        this.dimensionType = typeKey;
    }

    @Inject(method = "getDimensionFolder", cancellable = true, at = @At("HEAD"))
    private void arclight$useActualType(RegistryKey<World> dimensionKey, CallbackInfoReturnable<File> cir) {
        if (dimensionType == Dimension.OVERWORLD) {
            cir.setReturnValue(this.saveDir.toFile());
        } else if (dimensionType == Dimension.THE_NETHER) {
            cir.setReturnValue(new File(this.saveDir.toFile(), "DIM-1"));
        } else if (dimensionType == Dimension.THE_END) {
            cir.setReturnValue(new File(this.saveDir.toFile(), "DIM1"));
        }
    }
}
