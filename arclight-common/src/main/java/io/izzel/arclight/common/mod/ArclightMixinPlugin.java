package io.izzel.arclight.common.mod;

import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.common.mod.mixins.CreateConstructorProcessor;
import io.izzel.arclight.common.mod.mixins.InlineProcessor;
import io.izzel.arclight.common.mod.mixins.MixinProcessor;
import io.izzel.arclight.common.mod.mixins.RenameIntoProcessor;
import io.izzel.arclight.common.mod.mixins.TransformAccessProcessor;
import io.izzel.arclight.mixin.injector.EjectorInfo;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IEnvironmentTokenProvider;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;

import java.util.List;
import java.util.Set;

public class ArclightMixinPlugin implements IMixinConfigPlugin {

    private final List<MixinProcessor> preProcessors = List.of(
    );

    private final List<MixinProcessor> postProcessors = List.of(
        new RenameIntoProcessor(),
        new TransformAccessProcessor(),
        new CreateConstructorProcessor(),
        new InlineProcessor()
    );

    @Override
    public void onLoad(String mixinPackage) {
        InjectionInfo.register(EjectorInfo.class);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        for (var processor : this.preProcessors) {
            processor.accept(targetClassName, targetClass, mixinInfo);
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        for (var processor : this.postProcessors) {
            processor.accept(targetClassName, targetClass, mixinInfo);
        }
    }
}
