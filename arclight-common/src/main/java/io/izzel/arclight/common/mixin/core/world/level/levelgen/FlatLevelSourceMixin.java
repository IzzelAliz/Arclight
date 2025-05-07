package io.izzel.arclight.common.mixin.core.world.level.levelgen;

import io.izzel.arclight.common.bridge.core.world.level.levelgen.flat.FlatLevelGeneratorSettingsBridge;
import io.izzel.arclight.common.mod.mixins.annotation.CreateConstructor;
import io.izzel.arclight.common.mod.mixins.annotation.ShadowConstructor;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.function.Function;

@Mixin(FlatLevelSource.class)
public abstract class FlatLevelSourceMixin {

    @Shadow @Final @Mutable private FlatLevelGeneratorSettings settings;

    @ShadowConstructor.Super
    public abstract void arclight$constuctor$super(BiomeSource biomeSource, Function<Holder<Biome>, BiomeGenerationSettings> function);

    @CreateConstructor
    public void arclight$constructor(BiomeSource biomeSource, FlatLevelGeneratorSettings flatLevelGeneratorSettings) {
        arclight$constuctor$super(biomeSource, Util.memoize(flatLevelGeneratorSettings::adjustGenerationSettings));
        this.settings = flatLevelGeneratorSettings;
    }

    @ModifyArgs(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkGenerator;<init>(Lnet/minecraft/world/level/biome/BiomeSource;Ljava/util/function/Function;)V"))
    private static void arclight$useCustomBiomeSource(Args args, FlatLevelGeneratorSettings settings) {
        final var custom = ((FlatLevelGeneratorSettingsBridge) settings).bridge$getBiomeSource();
        if (custom != null) {
            args.set(0, custom);
        }
    }
}
