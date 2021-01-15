package io.izzel.arclight.common.mixin.core.world.gen.feature.structure;

import io.izzel.arclight.common.bridge.world.IWorldBridge;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.structure.StructureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(StructureManager.class)
public class StructureManagerMixin {

    @Shadow @Final private IWorld world;

    public World getWorld() {
        return ((IWorldBridge) this.world).bridge$getMinecraftWorld();
    }
}
