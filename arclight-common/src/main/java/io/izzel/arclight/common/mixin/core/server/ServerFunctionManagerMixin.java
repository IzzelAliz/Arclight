package io.izzel.arclight.common.mixin.core.server;

import com.mojang.brigadier.CommandDispatcher;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerFunctionManager.class)
public class ServerFunctionManagerMixin {

    @Shadow @Final MinecraftServer server;

    @Inject(method = "getDispatcher", cancellable = true, at = @At("HEAD"))
    private void arclight$useVanillaDispatcher(CallbackInfoReturnable<CommandDispatcher<CommandSourceStack>> cir) {
        cir.setReturnValue(((MinecraftServerBridge) this.server).bridge$getVanillaCommands().getDispatcher());
    }
}
