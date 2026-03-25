package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.core.server.level.ServerLevelBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import org.bukkit.GameRule;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

@Mixin(CraftWorld.class)
public abstract class CraftWorldMixin {

    // @formatter:off
    @Shadow @Final private ServerLevel world;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    public File getWorldFolder() {
        return ((ServerLevelBridge) this.world).bridge$getConvertable().getDimensionPath(this.world.dimension()).toFile();
    }

    @Inject(method = "convert", cancellable = true, at = @At("HEAD"), remap = false)
    private<T> void arclight$fallbackConvert(GameRule<T> rule, GameRules.Value<?> value, CallbackInfoReturnable<T> cir) {
        if (rule.getType() == String.class) {
            cir.setReturnValue(rule.getType().cast(value.serialize()));
        }
    }

    @Redirect(method = "getGameRuleValue(Ljava/lang/String;)Ljava/lang/String;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules$Value;toString()Ljava/lang/String;"))
    private String arclight$useSerialize(GameRules.Value<?> instance) {
        return instance.serialize();
    }
}
