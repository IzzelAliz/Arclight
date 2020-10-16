package io.izzel.arclight.common.mixin.forge;

import io.izzel.arclight.common.bridge.bukkit.CraftServerBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.DimensionManager;
import org.bukkit.Bukkit;
import org.bukkit.event.world.WorldUnloadEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DimensionManager.class, remap = false)
public abstract class DimensionManagerMixin {

    // @formatter:off
    @Shadow private static boolean canUnloadWorld(ServerWorld world) { return false; }
    // @formatter:on

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
