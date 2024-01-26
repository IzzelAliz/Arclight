package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.core.world.server.ServerWorldBridge;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;

@Mixin(value = CraftWorld.class, remap = false)
public abstract class CraftWorldMixin {

    // @formatter:off
    @Shadow @Final private ServerLevel world;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public File getWorldFolder() {
        return ((ServerWorldBridge) this.world).bridge$getConvertable().getDimensionPath(this.world.dimension()).toFile();
    }
}
