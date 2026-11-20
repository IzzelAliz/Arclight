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

    // Null until a MOTD is set through Bukkit or MinecraftServer#setMotd; the vanilla MOTD is used until then.
    // Since 26.3, DedicatedServer keeps the MOTD in its settings and overrides getMotd()/setMotd() without
    // calling super, so the setMotd injection below never runs on dedicated servers.
    @Unique private volatile Component arclight$motd;

    @Override
    public Component bridge$getMotd() {
        Component motd = this.arclight$motd;
        if (motd != null) return motd;
        return LegacyComponentSerializer.legacySection().deserializeOr(((MinecraftServer) (Object) this).getMotd(), Component.empty());
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
        Component motd = this.arclight$motd;
        return motd == null ? original : LegacyComponentSerializer.legacySection().serialize(motd);
    }

    @WrapOperation(method = "buildServerStatus()Lnet/minecraft/network/protocol/status/ServerStatus;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;nullToEmpty(Ljava/lang/String;)Lnet/minecraft/network/chat/Component;"),
        require = 1, expect = 1, allow = 1)
    private net.minecraft.network.chat.Component arclight$statusMotd(String legacy, Operation<net.minecraft.network.chat.Component> original) {
        return PaperAdventure.asVanilla(this.bridge$getMotd());
    }
}
