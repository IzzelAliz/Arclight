package io.izzel.arclight.common.mixin.core.server.commands;

import io.izzel.arclight.common.bridge.core.world.level.levelgen.StructureStartBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.bukkit.event.world.AsyncStructureGenerateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PlaceCommand.class)
public class PlaceCommandMixin {

    @Inject(method = "placeStructure", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/StructureStart;getBoundingBox()Lnet/minecraft/world/level/levelgen/structure/BoundingBox;"))
    private static void arclight$cause(CommandSourceStack p_214588_, Holder.Reference<Structure> p_251799_, BlockPos p_214590_, CallbackInfoReturnable<Integer> cir,
                                       ServerLevel level, Structure structure, ChunkGenerator chunkGenerator, StructureStart structureStart) {
        ((StructureStartBridge) (Object) structureStart).bridge$setGenerateCause(AsyncStructureGenerateEvent.Cause.COMMAND);
    }
}
