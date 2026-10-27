package io.izzel.arclight.common.mixin.core.server.commands;

import io.izzel.arclight.common.bridge.core.server.CommandResourceReloadBridge;
import io.izzel.arclight.common.bridge.core.server.packs.repository.PackRepositoryBridge;
import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.ReloadCommand;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.WorldData;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Mixin(ReloadCommand.class)
public abstract class ReloadCommandMixin {

    // @formatter:off
    @Shadow private static Collection<String> discoverNewPacks(PackRepository p_241058_0_, WorldData p_241058_1_, Collection<String> p_241058_2_) { return null; }
    // @formatter:on

    @Decorate(method = "reloadPacks(Ljava/util/Collection;Lnet/minecraft/commands/CommandSourceStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;reloadResources(Ljava/util/Collection;)Ljava/util/concurrent/CompletableFuture;"),
        require = 1, allow = 1)
    private static CompletableFuture<Void> arclight$commandResourceReload(MinecraftServer server,
            Collection<String> packs) throws Throwable {
        // Preserve the current native/mod call exactly once. Returning the composed
        // stage keeps command failure logging attached to both reload and notification.
        CompletableFuture<Void> reload = (CompletableFuture<Void>) DecorationOps.callsite().invoke(server, packs);
        return ((CommandResourceReloadBridge) server).bridge$notifyCommandResourceReload(reload);
    }

    @Redirect(method = "discoverNewPacks(Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/world/level/storage/WorldData;Ljava/util/Collection;)Ljava/util/Collection;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/PackRepository;reload()V"),
        require = 1, expect = 1, allow = 1)
    private static void arclight$discoverPendingReloadPacks(PackRepository repository) {
        ((PackRepositoryBridge) repository).bridge$reload(true);
    }

    @Decorate(method = "discoverNewPacks(Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/world/level/storage/WorldData;Ljava/util/Collection;)Ljava/util/Collection;",
        at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList(Ljava/lang/Iterable;)Ljava/util/ArrayList;", remap = false),
        require = 1, allow = 1)
    private static ArrayList<String> arclight$includePendingReloadPacks(Iterable<String> currentPacks,
            @Local(ordinal = 0) PackRepository repository) throws Throwable {
        // Both native callers pass the pre-reload selection. Seed from the rebuilt
        // selection instead: it also contains auto-enabled packs even when disabled
        // in world data. Merely updating repository.selected would not load them.
        // Keep the native new/disabled scan and NeoForge's subsequent reordering.
        return (ArrayList<String>) DecorationOps.callsite().invoke((Iterable<String>) repository.getSelectedIds());
    }

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static void reload(MinecraftServer minecraftserver) {
        PackRepository resourcePackList = minecraftserver.getPackRepository();
        WorldData configuration = minecraftserver.getWorldData();
        Collection<String> collection = resourcePackList.getSelectedIds();
        Collection<String> collection2 = discoverNewPacks(resourcePackList, configuration, collection);
        minecraftserver.reloadResources(collection2);
    }
}
