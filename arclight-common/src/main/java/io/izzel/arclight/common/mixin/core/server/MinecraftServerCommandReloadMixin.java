package io.izzel.arclight.common.mixin.core.server;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.core.server.ReloadableServerResourcesBridge;
import io.izzel.arclight.common.mod.util.PaperCommandLifecycle;
import io.papermc.paper.command.brigadier.PaperBrigadier;
import io.papermc.paper.command.brigadier.PaperCommands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.players.PlayerList;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.help.SimpleHelpMap;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerCommandReloadMixin {

    @Shadow private MinecraftServer.ReloadableResources resources;

    @Unique private boolean arclight$initialBootstrapCommandsFired;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$bindInitialPaperCommands(CallbackInfo ci) {
        // CraftCommandMap inserts defaults during CraftServer's field initializers,
        // before its constructor RETURN. The native server already owns this stem.
        arclight$bindPaperCommands(this.resources.managers());
    }

    @Inject(method = "setPlayerList(Lnet/minecraft/server/players/PlayerList;)V", at = @At("RETURN"),
        require = 1, expect = 1, allow = 1)
    private void arclight$initialBootstrapCommands(PlayerList playerList, CallbackInfo ci) {
        // PlayerList's constructor has now created CraftServer (including CommandMap
        // defaults), but DedicatedServer has not entered the main-plugin providers yet.
        if (this.arclight$initialBootstrapCommandsFired) {
            return;
        }
        this.arclight$initialBootstrapCommandsFired = true;
        PaperCommandLifecycle.bootstrap(ReloadableRegistrarEvent.Cause.INITIAL);
    }

    @Decorate(method = "lambda$reloadResources$4(Ljava/util/Collection;Lnet/minecraft/server/MinecraftServer$ReloadableResources;)V", at = @At(value = "FIELD",
        target = "Lnet/minecraft/server/MinecraftServer;resources:Lnet/minecraft/server/MinecraftServer$ReloadableResources;",
        opcode = Opcodes.PUTFIELD), require = 1, expect = 1, allow = 1)
    private void arclight$adoptPaperCommands(MinecraftServer server, MinecraftServer.ReloadableResources next) throws Throwable {
        Commands previousCommands = this.resources.managers().getCommands();
        // This is the native successful-load continuation, not loadResources RETURN
        // (which only returns a future). A failed load never reaches this field write.
        // Native code does not roll this assignment back if later finalization fails;
        // Paper must keep following the resources actually adopted in that case too.
        DecorationOps.callsite().invoke(server, next);
        arclight$bindPaperCommands(next.managers());
        // The forwarding CommandMap has no independent storage. Transfer its real
        // Bukkit nodes, including aliases/defaults, before the existing sync hook.
        // Do not construct another Commands: NeoForge registration runs only once.
        PaperBrigadier.moveBukkitCommands(previousCommands, next.managers().getCommands());
    }

    @Inject(method = "lambda$reloadResources$4(Ljava/util/Collection;Lnet/minecraft/server/MinecraftServer$ReloadableResources;)V", at = @At("RETURN"),
        require = 1, expect = 1, allow = 1)
    private void arclight$replayPaperCommands(java.util.Collection<String> packs,
            MinecraftServer.ReloadableResources next, CallbackInfo ci) {
        // Finish the native continuation first: loading, close, adoption and all
        // native reload finalizers must succeed before any plugin handler is replayed.
        // The adoption hook above has already restored defaults and legacy aliases.
        PaperCommandLifecycle.bootstrap(ReloadableRegistrarEvent.Cause.RELOAD);
        PaperCommandLifecycle.plugins(ReloadableRegistrarEvent.Cause.RELOAD);

        CraftServer craftServer = ((MinecraftServerBridge) this).bridge$getServer();
        SimpleHelpMap helpMap = (SimpleHelpMap) craftServer.getHelpMap();
        helpMap.clear();
        helpMap.initializeGeneralTopics();
        helpMap.initializeCommands();
        craftServer.syncCommands();
    }

    @Unique
    private static void arclight$bindPaperCommands(ReloadableServerResources resources) {
        PaperCommands.INSTANCE.setDispatcher(resources.getCommands(),
            ((ReloadableServerResourcesBridge) resources).bridge$getCommandBuildContext());
        try {
            io.papermc.paper.command.PaperCommands.registerCommands();
        } finally {
            // setDispatcher opens registration for the internal command setup above;
            // retained plugin registrars must stay closed until their event is fired.
            PaperCommands.INSTANCE.setCurrentContext(null);
            PaperCommands.INSTANCE.invalidate();
        }
    }
}
