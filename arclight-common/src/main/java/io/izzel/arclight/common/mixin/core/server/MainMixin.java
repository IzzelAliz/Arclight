package io.izzel.arclight.common.mixin.core.server;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.BukkitOptionParser;
import io.papermc.paper.plugin.PluginInitializerManager;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MainMixin {

    @Inject(method = "main([Ljava/lang/String;)V", at = @At("HEAD"), remap = false)
    private static void arclight$captureBukkitOptions(String[] args, CallbackInfo ci) {
        ArclightCaptures.captureBukkitOptions(new BukkitOptionParser().parse(args));
    }

    @Decorate(method = "main([Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Ljoptsimple/OptionParser;parse([Ljava/lang/String;)Ljoptsimple/OptionSet;"), remap = false, require = 1, expect = 1, allow = 1)
    private static OptionSet arclight$parseNativeOptions(OptionParser parser, String[] args) throws Throwable {
        return (OptionSet) DecorationOps.callsite().invoke(parser, BukkitOptionParser.filterNativeArguments(parser, ArclightCaptures.getBukkitOptions(), args));
    }

    @Inject(method = "main([Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/Bootstrap;bootStrap()V"), remap = false)
    private static void arclight$loadPluginProviders(String[] args, CallbackInfo ci) throws Exception {
        PluginInitializerManager.load(ArclightCaptures.getBukkitOptions());
    }
}