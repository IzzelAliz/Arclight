package io.izzel.arclight.common.mod.util;

import com.mojang.brigadier.tree.CommandNode;
import io.papermc.paper.command.brigadier.ApiMirrorRootNode;
import io.papermc.paper.command.brigadier.PaperBrigadier;
import io.papermc.paper.command.brigadier.PaperCommands;
import io.papermc.paper.command.brigadier.bukkit.BukkitCommandNode;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventOwner;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventRunner;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;

import java.util.List;

/** Dispatches COMMANDS against the already adopted native command dispatcher. */
public final class PaperCommandLifecycle {

    private PaperCommandLifecycle() {
    }

    public static void bootstrap(ReloadableRegistrarEvent.Cause cause) {
        // Upstream builds bootstrap commands before moving the old Bukkit nodes.
        // Our successful-adoption-only dispatch runs later, so preserve that same
        // precedence without moving nodes twice or losing defaults/legacy aliases.
        var root = ((ApiMirrorRootNode) PaperCommands.INSTANCE.getDispatcherInternal().getRoot())
            .getDispatcher().getRoot();
        List<CommandNode<net.minecraft.commands.CommandSourceStack>> bukkitNodes =
            root.getChildren().stream()
                .filter(node -> ((CommandNode<?>) node).getCommand() instanceof BukkitCommandNode.BukkitBrigCommand)
                .toList();
        try {
            dispatch(BootstrapContext.class, cause);
        } finally {
            for (var node : bukkitNodes) {
                if (root.getChild(node.getName()) != node) {
                    PaperBrigadier.removeCommand(root, node.getName());
                    root.addChild(node);
                }
            }
        }
    }

    /** Call after both main-plugin enable phases, before aliases/help and client sync. */
    public static void plugins(ReloadableRegistrarEvent.Cause cause) {
        dispatch(Plugin.class, cause);
    }

    private static void dispatch(Class<? extends LifecycleEventOwner> ownerClass,
            ReloadableRegistrarEvent.Cause cause) {
        PaperCommands registrar = PaperCommands.INSTANCE;
        registrar.setValid();
        try {
            // The real runner supplies each handler's owner, so implicit registration
            // uses that owner's PluginMeta and the runner retains priority ordering.
            LifecycleEventRunner.INSTANCE.callReloadableRegistrarEvent(
                LifecycleEvents.COMMANDS, registrar, ownerClass, cause);
        } finally {
            // The runner clears each owner, but invalidates only on normal completion.
            // Do not leave a captured registrar usable after an exceptional handler.
            registrar.setCurrentContext(null);
            registrar.invalidate();
        }
    }
}
