package io.izzel.arclight.common.mixin.core.server.jsonrpc;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.izzel.arclight.common.bridge.core.world.level.GameRulesBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.internalapi.MinecraftGameRuleServiceImpl;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftGameRuleServiceImpl.class)
public abstract class MinecraftGameRuleServiceMixin {
    @Shadow private DedicatedServer server() { return null; }

    @ModifyExpressionValue(method = {"updateGameRule", "getAvailableGameRules", "getRuleValue"},
        at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getGameRules()Lnet/minecraft/world/level/gamerules/GameRules;"),
            @At(value = "INVOKE", target = "Lnet/minecraft/server/dedicated/DedicatedServer;getGameRules()Lnet/minecraft/world/level/gamerules/GameRules;")
        }, require = 3, allow = 3)
    private GameRules arclight$liveWorldRules(GameRules cached) {
        return this.server().overworld().getGameRules();
    }

    @Redirect(method = "updateGameRule", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gamerules/GameRules;set(Lnet/minecraft/world/level/gamerules/GameRule;Ljava/lang/Object;Lnet/minecraft/server/MinecraftServer;)V"))
    private <T> void arclight$setWorldRule(GameRules rules, GameRule<T> rule, T value, MinecraftServer server) {
        ((GameRulesBridge) rules).bridge$setWorldRule(rule, value, server.overworld());
    }
}
