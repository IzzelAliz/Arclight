package io.izzel.arclight.common.mixin.core.world.gen.feature.template;

import com.mojang.datafixers.DataFixer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelStorageSource;

@Mixin(StructureManager.class)
public class TemplateManagerMixin {

    @Shadow @Final @Mutable private Map<ResourceLocation, StructureTemplate> structureRepository;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(ResourceManager p_i232119_1_, LevelStorageSource.LevelStorageAccess p_i232119_2_, DataFixer fixer, CallbackInfo ci) {
        this.structureRepository = Collections.synchronizedMap(this.structureRepository);
    }
}
