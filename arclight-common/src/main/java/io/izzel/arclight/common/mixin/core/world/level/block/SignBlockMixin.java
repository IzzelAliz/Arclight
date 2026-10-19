package io.izzel.arclight.common.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.world.level.block.SignBlockEditBridge;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.player.PlayerSignOpenEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignBlock.class)
public abstract class SignBlockMixin implements SignBlockEditBridge {

    // @formatter:off
    @Shadow public abstract void openTextEdit(Player p_277738_, SignBlockEntity p_277467_, boolean p_277771_);
    // @formatter:on

    private transient PlayerSignOpenEvent.Cause arclight$edit;
    private transient Player arclight$editPlayer;
    private transient SignBlockEntity arclight$editSign;
    private transient boolean arclight$editFront;

    @WrapOperation(method = "useWithoutItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/SignBlock;openTextEdit(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/entity/SignBlockEntity;Z)V"))
    private void arclight$interactEdit(SignBlock block, Player player, SignBlockEntity sign, boolean isFrontText, Operation<Void> original) {
        var previous = arclight$edit;
        var previousPlayer = arclight$editPlayer;
        var previousSign = arclight$editSign;
        boolean previousFront = arclight$editFront;
        arclight$edit = PlayerSignOpenEvent.Cause.INTERACT;
        arclight$editPlayer = player;
        arclight$editSign = sign;
        arclight$editFront = isFrontText;
        try {
            original.call(block, player, sign, isFrontText);
        } finally {
            arclight$edit = previous;
            arclight$editPlayer = previousPlayer;
            arclight$editSign = previousSign;
            arclight$editFront = previousFront;
        }
    }

    public void openTextEdit(Player player, SignBlockEntity sign, boolean isFrontText, PlayerSignOpenEvent.Cause cause) {
        var previous = arclight$edit;
        var previousPlayer = arclight$editPlayer;
        var previousSign = arclight$editSign;
        boolean previousFront = arclight$editFront;
        arclight$edit = cause;
        arclight$editPlayer = player;
        arclight$editSign = sign;
        arclight$editFront = isFrontText;
        try {
            this.openTextEdit(player, sign, isFrontText);
        } finally {
            arclight$edit = previous;
            arclight$editPlayer = previousPlayer;
            arclight$editSign = previousSign;
            arclight$editFront = previousFront;
        }
    }

    @Override
    public void bridge$openTextEdit(Player player, SignBlockEntity sign, boolean front,
                                   PlayerSignOpenEvent.Cause cause, Operation<Void> original) {
        var previous = arclight$edit;
        var previousPlayer = arclight$editPlayer;
        var previousSign = arclight$editSign;
        boolean previousFront = arclight$editFront;
        arclight$edit = cause;
        arclight$editPlayer = player;
        arclight$editSign = sign;
        arclight$editFront = front;
        try {
            original.call((SignBlock) (Object) this, player, sign, front);
        } finally {
            arclight$edit = previous;
            arclight$editPlayer = previousPlayer;
            arclight$editSign = previousSign;
            arclight$editFront = previousFront;
        }
    }

    @Inject(method = "openTextEdit(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/entity/SignBlockEntity;Z)V", cancellable = true, at = @At("HEAD"))
    private void arclight$signEdit(Player player, SignBlockEntity signBlockEntity, boolean flag, CallbackInfo ci) {
        boolean owned = arclight$edit != null && (arclight$editSign == null
            || (arclight$editPlayer == player && arclight$editSign == signBlockEntity && arclight$editFront == flag));
        var cause = owned ? arclight$edit : PlayerSignOpenEvent.Cause.UNKNOWN;
        var pendingCause = arclight$edit;
        var pendingPlayer = arclight$editPlayer;
        var pendingSign = arclight$editSign;
        boolean pendingFront = arclight$editFront;
        // Mask even a mismatching owner's event callbacks: listeners cannot borrow it.
        // A matching native entry consumes it; a mismatch restores it after dispatch.
        // Identity-identical pre-continuation opens inside an opaque third-party Operation
        // cannot be distinguished by this native ABI (see the revision compatibility report).
        arclight$edit = null;
        arclight$editPlayer = null;
        arclight$editSign = null;
        try {
            if (!CraftEventFactory.callPlayerSignOpenEvent(player, signBlockEntity, flag, cause)) {
                ci.cancel();
            }
        } finally {
            if (!owned) {
                arclight$edit = pendingCause;
                arclight$editPlayer = pendingPlayer;
                arclight$editSign = pendingSign;
                arclight$editFront = pendingFront;
            }
        }
    }
}
