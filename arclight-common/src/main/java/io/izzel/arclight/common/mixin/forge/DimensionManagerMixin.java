package io.izzel.arclight.common.mixin.forge;

import io.izzel.arclight.common.bridge.bukkit.CraftServerBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.DimensionManager;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = DimensionManager.class, remap = false)
public abstract class DimensionManagerMixin {

    // @formatter:off
    @Shadow private static boolean canUnloadWorld(ServerWorld world) { return false; }
    // @formatter:on

    @Inject(method = "initWorld", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z"))
    private static void arclight$updateWorldInit(MinecraftServer server, DimensionType dim, CallbackInfoReturnable<ServerWorld> cir, ServerWorld overworld, ServerWorld world) {
        CraftWorld craftWorld = ((WorldBridge) world).bridge$getWorld();
        if (((WorldBridge) world).bridge$getGenerator() != null) {
            craftWorld.getPopulators().addAll(((WorldBridge) world).bridge$getGenerator().getDefaultPopulators(craftWorld));
        }
        Bukkit.getPluginManager().callEvent(new WorldInitEvent(craftWorld));
        Bukkit.getPluginManager().callEvent(new WorldLoadEvent(craftWorld));
    }

    @Redirect(method = "unloadWorlds", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/DimensionManager;canUnloadWorld(Lnet/minecraft/world/server/ServerWorld;)Z"))
    private static boolean arclight$updateWorldMap(ServerWorld world) {
        boolean unload = canUnloadWorld(world);
        if (unload) {
            WorldUnloadEvent event = new WorldUnloadEvent(((WorldBridge) world).bridge$getWorld());
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                ((CraftServerBridge) Bukkit.getServer()).bridge$removeWorld(world);
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}
