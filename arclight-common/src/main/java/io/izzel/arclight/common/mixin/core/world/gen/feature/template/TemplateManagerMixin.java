package io.izzel.arclight.common.mixin.core.world.gen.feature.template;

import com.mojang.datafixers.DataFixer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.gen.feature.template.TemplateManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.Collections;
import java.util.Map;

@Mixin(TemplateManager.class)
public class TemplateManagerMixin {

    @Shadow @Final @Mutable private Map<ResourceLocation, Template> templates;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(MinecraftServer server, File templateFolder, DataFixer fixerIn, CallbackInfo ci) {
        this.templates = Collections.synchronizedMap(this.templates);
    }
}
