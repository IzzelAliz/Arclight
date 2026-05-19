package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.core.server.level.ServerLevelBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;

@Mixin(CraftWorld.class)
public abstract class CraftWorldMixin {

    // @formatter:off
    @Shadow @Final private ServerLevel world;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    public File getWorldFolder() {
        return ((ServerLevelBridge) this.world).bridge$getConvertable().getDimensionPath(this.world.dimension()).toFile();
    }

    /**
     * @author IzzelAliz
     * @reason avoid direct CraftBukkit field access to ServerLevel.serverLevelData on remapped runtime
     */
    @Overwrite(remap = false)
    public String getName() {
        return ((ServerLevelData) this.world.getLevelData()).getLevelName();
    }

}
