package io.izzel.arclight.neoforge.mixin.core.world.level;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRules.class, priority = 450)
public abstract class GameRulesMixin_NeoForge {
    @Inject(method = "set(Lnet/minecraft/world/level/gamerules/GameRule;Ljava/lang/Object;Lnet/minecraft/server/level/ServerLevel;)V",
        at = @At(value = "RETURN", ordinal = 2), require = 1, expect = 1, allow = 1, remap = false)
    private <T> void arclight$onWorldRuleChanged(GameRule<T> rule, T value, ServerLevel level, CallbackInfo ci) {
        EventHooks.onGameRuleChanged(level.getServer(), rule, value);
    }
}
