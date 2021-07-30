package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.block.NoteBlockBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
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
    @Shadow protected abstract void playNote(Level worldIn, BlockPos pos);
    // @formatter:on

    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/NoteBlock;playNote(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    public void arclight$callNote2(NoteBlock noteBlock, Level worldIn, BlockPos pos, BlockState blockState) {
        this.bridge$play(worldIn, pos, blockState);
    }

    @Redirect(method = "neighborChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/NoteBlock;playNote(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    public void arclight$callNote1(NoteBlock noteBlock, Level worldIn, BlockPos pos, BlockState blockState) {
        this.play(worldIn, pos, blockState);
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/NoteBlock;playNote(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    public void arclight$callNote3(NoteBlock noteBlock, Level worldIn, BlockPos pos, BlockState blockState) {
        this.play(worldIn, pos, blockState);
    }

    private transient BlockState arclight$state;

    private void play(Level worldIn, BlockPos pos, BlockState state) {
        arclight$state = state;
        this.playNote(worldIn, pos);
        arclight$state = null;
    }

    @Override
    public void bridge$play(Level worldIn, BlockPos pos, BlockState state) {
        this.play(worldIn, pos, state);
    }

    @Inject(method = "playNote", cancellable = true, require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;blockEvent(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;II)V"))
    private void arclight$notePlay(Level worldIn, BlockPos pos, CallbackInfo ci) {
        NotePlayEvent event = CraftEventFactory.callNotePlayEvent(worldIn, pos, arclight$state.getValue(NoteBlock.INSTRUMENT), arclight$state.getValue(NoteBlock.NOTE));
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
