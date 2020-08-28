package io.izzel.arclight.common.mixin.core.world.gen.feature.structure;

import io.izzel.arclight.common.bridge.world.IWorldWriterBridge;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.structure.SwampHutPiece;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(SwampHutPiece.class)
public class SwampHutPieceMixin {

    @Inject(method = "func_230383_a_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ISeedReader;func_242417_l(Lnet/minecraft/entity/Entity;)V"))
    private void arclight$spawnReason1(ISeedReader worldIn, StructureManager p_230383_2_, ChunkGenerator p_230383_3_, Random p_230383_4_, MutableBoundingBox p_230383_5_, ChunkPos p_230383_6_, BlockPos p_230383_7_, CallbackInfoReturnable<Boolean> cir) {
        ((IWorldWriterBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.CHUNK_GEN);
    }

    @Inject(method = "func_214821_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IServerWorld;func_242417_l(Lnet/minecraft/entity/Entity;)V"))
    private void arclight$spawnReason2(IServerWorld worldIn, MutableBoundingBox p_214821_2_, CallbackInfo ci) {
        ((IWorldWriterBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.CHUNK_GEN);
    }
}
