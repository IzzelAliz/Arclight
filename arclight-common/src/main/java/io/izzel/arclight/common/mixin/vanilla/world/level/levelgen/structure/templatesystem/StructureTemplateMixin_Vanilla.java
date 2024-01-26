package io.izzel.arclight.common.mixin.vanilla.world.level.levelgen.structure.templatesystem;

import io.izzel.arclight.common.bridge.core.world.level.levelgen.structure.templatesystem.StructureTemplateBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin_Vanilla implements StructureTemplateBridge {

    // @formatter:off
    @Shadow protected abstract void placeEntities(ServerLevelAccessor serverLevelAccessor, BlockPos blockPos, Mirror mirror, Rotation rotation, BlockPos blockPos2, @Nullable BoundingBox boundingBox, boolean bl);
    // @formatter:on

    @Override
    public void bridge$platform$placeEntities(ServerLevelAccessor serverLevelAccessor, BlockPos blockPos, Mirror mirror, Rotation rotation, BlockPos blockPos2, @Nullable BoundingBox boundingBox, boolean bl, StructurePlaceSettings placementIn) {
        this.placeEntities(serverLevelAccessor, blockPos, mirror, rotation, blockPos2, boundingBox, bl);
    }
}
