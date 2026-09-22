package io.izzel.arclight.common.mixin.core.world.level.chunk.storage;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DataFixer;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.structure.LegacyStructureDataHandler;
import net.minecraft.world.level.chunk.storage.LegacyTagFixer;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.function.Supplier;

@Mixin(ChunkMap.class)
public abstract class ChunkStorageMixin {

    @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/LegacyStructureDataHandler;getLegacyTagFixer(Lnet/minecraft/resources/ResourceKey;Ljava/util/function/Supplier;Lcom/mojang/datafixers/DataFixer;)Ljava/util/function/Supplier;"))
    private static Supplier<LegacyTagFixer> arclight$legacyType(Supplier<LegacyTagFixer> original,
            @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) DataFixer fixer,
            @Local(argsOnly = true) Supplier<DimensionDataStorage> storage) {
        var dimension = level.dimension();
        // ServerLevel assigns its Bukkit type key after constructing ChunkMap.
        return () -> {
            var type = ((WorldBridge) level).bridge$getTypeKey();
            if (type == null) {
                return original.get();
            }
            if (type == LevelStem.OVERWORLD) {
                if (dimension == Level.OVERWORLD) return original.get();
                return new LegacyStructureDataHandler(storage.get(), ImmutableList.of("Monument", "Stronghold", "Village", "Mineshaft", "Temple", "Mansion"), ImmutableList.of("Village", "Mineshaft", "Mansion", "Igloo", "Desert_Pyramid", "Jungle_Pyramid", "Swamp_Hut", "Stronghold", "Monument"), fixer);
            } else if (type == LevelStem.NETHER) {
                if (dimension == Level.NETHER) return original.get();
                List<String> keys = ImmutableList.of("Fortress");
                return new LegacyStructureDataHandler(storage.get(), keys, keys, fixer);
            } else if (type == LevelStem.END) {
                if (dimension == Level.END) return original.get();
                List<String> keys = ImmutableList.of("EndCity");
                return new LegacyStructureDataHandler(storage.get(), keys, keys, fixer);
            }
            return LegacyTagFixer.EMPTY.get();
        };
    }
}
