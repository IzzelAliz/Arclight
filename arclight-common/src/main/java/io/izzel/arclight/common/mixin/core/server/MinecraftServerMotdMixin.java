package io.izzel.arclight.common.mixin.core.server;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMotdMixin implements MinecraftServerBridge {

    @Shadow private String motd;

    @Unique private volatile Component arclight$motd = Component.empty();

    @Override
    public Component bridge$getMotd() {
        return this.arclight$motd;
    }

    @Override
    public void bridge$setMotd(Component motd) {
        Component checked = Objects.requireNonNull(motd, "motd");
        this.motd = LegacyComponentSerializer.legacySection().serialize(checked);
        this.arclight$motd = checked;
    }

    @Inject(method = "setMotd(Ljava/lang/String;)V", at = @At("RETURN"), require = 1)
    private void arclight$readLegacyMotd(String motd, CallbackInfo ci) {
        this.arclight$motd = LegacyComponentSerializer.legacySection().deserializeOr(this.motd, Component.empty());
    }

    @ModifyReturnValue(method = "getMotd()Ljava/lang/String;", at = @At("RETURN"), require = 1)
    private String arclight$legacyMotd(String original) {
        return LegacyComponentSerializer.legacySection().serialize(this.arclight$motd);
    }

    @WrapOperation(method = "buildServerStatus()Lnet/minecraft/network/protocol/status/ServerStatus;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;nullToEmpty(Ljava/lang/String;)Lnet/minecraft/network/chat/Component;"),
        require = 1, expect = 1, allow = 1)
    private net.minecraft.network.chat.Component arclight$statusMotd(String legacy, Operation<net.minecraft.network.chat.Component> original) {
        return PaperAdventure.asVanilla(this.arclight$motd);
    }
}
