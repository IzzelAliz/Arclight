package io.izzel.arclight.common.mixin.v1_15.block;

import io.izzel.arclight.common.bridge.block.NoteBlockBridge;
import net.minecraft.block.BlockState;
import net.minecraft.block.NoteBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NoteBlock.class)
public abstract class NoteBlockMixin_1_15 implements NoteBlockBridge {

    @Redirect(method = "onBlockActivated", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/NoteBlock;triggerNote(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"))
    public void arclight$callNote2(NoteBlock noteBlock, World worldIn, BlockPos pos, BlockState blockState) {
        this.bridge$play(worldIn, pos, blockState);
    }
}
