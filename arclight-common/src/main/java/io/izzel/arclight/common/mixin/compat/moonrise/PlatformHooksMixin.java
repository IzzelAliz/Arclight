package io.izzel.arclight.common.mixin.compat.moonrise;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.izzel.arclight.common.bridge.core.world.level.chunk.LevelChunkBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = {"ca.spottedleaf.moonrise.fabric.FabricHooks", "ca.spottedleaf.moonrise.neoforge.NeoForgeHooks"}, remap = false)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.PRESENT)
public abstract class PlatformHooksMixin {
    @ModifyReturnValue(method = "forceNoSave(Lnet/minecraft/world/level/chunk/ChunkAccess;)Z", at = @At("RETURN"), remap = false)
    private boolean arclight$savePolicy(boolean original, ChunkAccess chunk) {
        return original || chunk instanceof LevelChunk && ((LevelChunkBridge) chunk).bridge$isMustNotSave();
    }
}
