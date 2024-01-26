package io.izzel.arclight.common.mod;

import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.common.mod.mixins.CreateConstructorProcessor;
import io.izzel.arclight.common.mod.mixins.MixinProcessor;
import io.izzel.arclight.common.mod.mixins.RenameIntoProcessor;
import io.izzel.arclight.common.mod.mixins.TransformAccessProcessor;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IEnvironmentTokenProvider;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class ArclightMixinPlugin implements IMixinConfigPlugin, IEnvironmentTokenProvider {

    private final List<MixinProcessor> preProcessors = List.of(
    );

    private final List<MixinProcessor> postProcessors = List.of(
        new RenameIntoProcessor(),
        new TransformAccessProcessor(),
        new CreateConstructorProcessor()
    );

    @Override
    public void onLoad(String mixinPackage) {
        MixinEnvironment.getCurrentEnvironment().registerTokenProvider(this);
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

    @Override
    public int getPriority() {
        return 500;
    }

    @Override
    public Integer getToken(String token, MixinEnvironment env) {
        if (token.startsWith("AP_")) {
            var platform = token.substring(3);
            return ArclightPlatform.current().name().equals(platform) ? 1 : 0;
        }
        return null;
    }
}
