package io.izzel.arclight.common.mixin.bukkit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.craftbukkit.v.CraftChunk;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CraftChunk.class, remap = false)
public abstract class CraftChunkMixin {

    @Redirect(method = "<init>*", remap = false, at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/world/level/chunk/LevelChunk;level:Lnet/minecraft/server/level/ServerLevel;", remap = true))
    private ServerLevel arclight$level(LevelChunk levelChunk) {
        return (ServerLevel) levelChunk.getLevel();
    }
}
