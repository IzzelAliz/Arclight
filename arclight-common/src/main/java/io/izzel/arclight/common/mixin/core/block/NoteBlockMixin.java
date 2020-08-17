package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.block.NoteBlockBridge;
import net.minecraft.block.BlockState;
import net.minecraft.block.NoteBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.NotePlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoteBlock.class)
public abstract class NoteBlockMixin implements NoteBlockBridge {

    // @formatter:off
    @Shadow protected abstract void triggerNote(World worldIn, BlockPos pos);
    // @formatter:on

    @Redirect(method = "onBlockActivated", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/NoteBlock;triggerNote(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"))
    public void arclight$callNote2(NoteBlock noteBlock, World worldIn, BlockPos pos, BlockState blockState) {
        this.bridge$play(worldIn, pos, blockState);
    }

    @Redirect(method = "neighborChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/NoteBlock;triggerNote(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"))
    public void arclight$callNote1(NoteBlock noteBlock, World worldIn, BlockPos pos, BlockState blockState) {
        this.play(worldIn, pos, blockState);
    }

    @Redirect(method = "onBlockClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/NoteBlock;triggerNote(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"))
    public void arclight$callNote3(NoteBlock noteBlock, World worldIn, BlockPos pos, BlockState blockState) {
        this.play(worldIn, pos, blockState);
    }

    private transient BlockState arclight$state;

    private void play(World worldIn, BlockPos pos, BlockState state) {
        arclight$state = state;
        this.triggerNote(worldIn, pos);
        arclight$state = null;
    }

    @Override
    public void bridge$play(World worldIn, BlockPos pos, BlockState state) {
        this.play(worldIn, pos, state);
    }

    @Inject(method = "triggerNote", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addBlockEvent(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;II)V"))
    private void arclight$notePlay(World worldIn, BlockPos pos, CallbackInfo ci) {
        NotePlayEvent event = CraftEventFactory.callNotePlayEvent(worldIn, pos, arclight$state.get(NoteBlock.INSTRUMENT), arclight$state.get(NoteBlock.NOTE));
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
