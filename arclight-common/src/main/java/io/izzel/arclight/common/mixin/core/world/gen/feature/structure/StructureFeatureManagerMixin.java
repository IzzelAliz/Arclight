package io.izzel.arclight.common.mixin.core.world.gen.feature.structure;

import io.izzel.arclight.common.bridge.core.world.IWorldBridge;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(StructureFeatureManager.class)
public class StructureFeatureManagerMixin {

    @Shadow @Final private LevelAccessor level;

    public Level getWorld() {
        return ((IWorldBridge) this.level).bridge$getMinecraftWorld();
    }
}
