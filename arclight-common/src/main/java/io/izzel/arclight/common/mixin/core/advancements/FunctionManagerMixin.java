package io.izzel.arclight.common.mixin.core.advancements;

import com.mojang.brigadier.CommandDispatcher;
import io.izzel.arclight.common.bridge.server.MinecraftServerBridge;
import net.minecraft.advancements.FunctionManager;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FunctionManager.class)
public class FunctionManagerMixin {

    @Shadow @Final private MinecraftServer server;

    @Inject(method = "getCommandDispatcher", cancellable = true, at = @At("HEAD"))
    private void arclight$useVanillaDispatcher(CallbackInfoReturnable<CommandDispatcher<CommandSource>> cir) {
        cir.setReturnValue(((MinecraftServerBridge) this.server).bridge$getVanillaCommands().getDispatcher());
    }
}
