package io.izzel.arclight.common.mixin.core.server.level;

import com.google.common.collect.Lists;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.storage.MapDataBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/server/level/ServerLevel$EntityCallbacks")
public class ServerLevel_EntityCallbacksMixin {

    @Inject(method = "onTrackingStart(Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"))
    private void arclight$valid(Entity entity, CallbackInfo ci) {
        ((EntityBridge) entity).bridge$setValid(true);
    }

    @Inject(method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void arclight$entityCleanup(Entity entity, CallbackInfo ci) {
        if (entity instanceof Player player) {
            for (ServerLevel serverLevel : ServerLifecycleHooks.getCurrentServer().levels.values()) {
                DimensionDataStorage worldData = serverLevel.getDataStorage();
                for (Object o : worldData.cache.values()) {
                    if (o instanceof MapItemSavedData map) {
                        map.carriedByPlayers.remove(player);
                        ((MapDataBridge) map).bridge$getCarriedBy().removeIf(holdingPlayer -> holdingPlayer.player == entity);
                    }
                }
            }
        }
        if (((EntityBridge) entity).bridge$getBukkitEntity() instanceof InventoryHolder holder) {
            for (org.bukkit.entity.HumanEntity h : Lists.newArrayList(holder.getInventory().getViewers())) {
                h.closeInventory();
            }
        }
    }

    @Inject(method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"))
    private void arclight$invalid(Entity entity, CallbackInfo ci) {
        ((EntityBridge) entity).bridge$setValid(true);
    }
}
