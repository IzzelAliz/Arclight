package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.NotePlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoteBlock.class)
public abstract class NoteBlockMixin {

    @Inject(method = "playNote", cancellable = true, require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;blockEvent(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;II)V"))
    private void arclight$notePlay(Entity entity, BlockState state, Level worldIn, BlockPos pos, CallbackInfo ci) {
        NotePlayEvent event = CraftEventFactory.callNotePlayEvent(worldIn, pos, state.getValue(NoteBlock.INSTRUMENT), state.getValue(NoteBlock.NOTE));
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
