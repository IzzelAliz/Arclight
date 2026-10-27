package io.izzel.arclight.common.mixin.core.server;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.bridge.core.server.ServerTickRateManagerBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerTickRateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ServerTickRateManager.class)
public abstract class ServerTickRateManagerMixin implements ServerTickRateManagerBridge {

    // @formatter:off
    @Shadow public abstract boolean isSprinting();
    @Shadow public abstract boolean requestGameToSprint(int time);
    @Shadow public abstract boolean stopSprinting();
    // @formatter:on

    private boolean arclight$sendLog = true;
    private boolean arclight$silent;

    @Override
    public boolean bridge$requestGameToSprint(int time, boolean silent) {
        boolean sprinting = this.isSprinting();
        boolean interrupted = this.requestGameToSprint(time);
        if (!sprinting) {
            this.arclight$silent = silent;
        }
        return interrupted;
    }

    @WrapMethod(method = "requestGameToSprint(I)Z")
    private boolean arclight$requestSprint(int time, Operation<Boolean> original) {
        boolean sprinting = this.isSprinting();
        boolean silent = this.arclight$silent;
        boolean interrupted = original.call(time);
        // Keep any old-session report in the native request on its original flag.
        // A direct one-argument request starts a non-silent session by default.
        this.arclight$silent = sprinting && silent;
        return interrupted;
    }

    @Inject(method = "finishTickSprint()V", at = @At("RETURN"))
    private void arclight$finishSprint(CallbackInfo ci) {
        this.arclight$silent = false;
    }

    public boolean stopSprinting(boolean sendLog) {
        try {
            arclight$sendLog = sendLog;
            return this.stopSprinting();
        } finally {
            arclight$sendLog = true;
        }
    }

    @Redirect(method = "finishTickSprint", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/CommandSourceStack;sendSuccess(Ljava/util/function/Supplier;Z)V"))
    private void arclight$send(CommandSourceStack source, Supplier<Component> supplier, boolean flag) {
        if (this.arclight$sendLog && !this.arclight$silent) {
            source.sendSuccess(supplier, flag);
        }
    }

    public boolean requestGameToSprint(int time, boolean silent) {
        return this.bridge$requestGameToSprint(time, silent);
    }

}
