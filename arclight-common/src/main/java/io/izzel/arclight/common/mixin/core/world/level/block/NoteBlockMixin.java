package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.block.NotePlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoteBlock.class)
public abstract class NoteBlockMixin {

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "neighborChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/NoteBlock;playNote(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void arclight$refreshAfterPlay(NoteBlock block, Entity entity, BlockState current, Level level, BlockPos pos,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) com.llamalad7.mixinextras.sugar.ref.LocalRef<BlockState> state) {
        original.call(block, entity, current, level, pos);
        state.set(level.getBlockState(pos));
    }


    @Inject(method = "playNote", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;blockEvent(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;II)V"))
    private void arclight$notePlay(Entity entity, BlockState state, Level worldIn, BlockPos pos, CallbackInfo ci) {
        NotePlayEvent event = CraftEventFactory.callNotePlayEvent(worldIn, pos, state.getValue(NoteBlock.INSTRUMENT), state.getValue(NoteBlock.NOTE));
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
