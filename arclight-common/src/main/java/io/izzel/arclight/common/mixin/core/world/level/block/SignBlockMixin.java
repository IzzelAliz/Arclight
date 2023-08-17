package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.player.PlayerSignOpenEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SignBlock.class)
public abstract class SignBlockMixin {

    // @formatter:off
    @Shadow public abstract void openTextEdit(Player p_277738_, SignBlockEntity p_277467_, boolean p_277771_);
    // @formatter:on

    private transient PlayerSignOpenEvent.Cause arclight$edit;

    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/SignBlock;openTextEdit(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/entity/SignBlockEntity;Z)V"))
    private void arclight$beforeEdit(BlockState p_56278_, Level p_56279_, BlockPos p_56280_, Player p_56281_, InteractionHand p_56282_, BlockHitResult p_56283_, CallbackInfoReturnable<InteractionResult> cir) {
        arclight$edit = PlayerSignOpenEvent.Cause.INTERACT;
    }

    @Inject(method = "use", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/block/SignBlock;openTextEdit(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/entity/SignBlockEntity;Z)V"))
    private void arclight$afterEdit(BlockState p_56278_, Level p_56279_, BlockPos p_56280_, Player p_56281_, InteractionHand p_56282_, BlockHitResult p_56283_, CallbackInfoReturnable<InteractionResult> cir) {
        arclight$edit = null;
    }

    public void openTextEdit(Player p_277738_, SignBlockEntity p_277467_, boolean p_277771_, PlayerSignOpenEvent.Cause cause) {
        arclight$edit = cause;
        this.openTextEdit(p_277738_, p_277467_, p_277771_);
        arclight$edit = null;
    }

    @Inject(method = "openTextEdit", cancellable = true, at = @At("HEAD"))
    private void arclight$signEdit(Player player, SignBlockEntity signBlockEntity, boolean flag, CallbackInfo ci) {
        if (!CraftEventFactory.callPlayerSignOpenEvent(player, signBlockEntity, flag, arclight$edit != null ? arclight$edit : (flag ? PlayerSignOpenEvent.Cause.PLACE : PlayerSignOpenEvent.Cause.UNKNOWN))) {
            ci.cancel();
        }
    }
}
