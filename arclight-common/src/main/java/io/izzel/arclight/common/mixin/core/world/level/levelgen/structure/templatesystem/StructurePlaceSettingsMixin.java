package io.izzel.arclight.common.mixin.core.world.level.levelgen.structure.templatesystem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(StructurePlaceSettings.class)
public class StructurePlaceSettingsMixin {

    @Shadow private int palette;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(CallbackInfo ci) {
        this.palette = -1;
    }

    @Inject(method = "getRandomPalette", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;getRandom(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/util/RandomSource;"))
    private void arclight$forcePalette(List<StructureTemplate.Palette> list, BlockPos p_74389_, CallbackInfoReturnable<StructureTemplate.Palette> cir) {
        var i = list.size();
        if (this.palette > 0) {
            if (this.palette >= i) {
                throw new IllegalArgumentException("Palette index out of bounds. Got " + this.palette + " where there are only " + i + " palettes available.");
            }
            cir.setReturnValue(list.get(this.palette));
        }
    }
}
