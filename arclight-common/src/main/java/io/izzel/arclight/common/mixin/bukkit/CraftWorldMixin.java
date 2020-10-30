package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.EntityTypeBridge;
import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.util.function.Function;

@Mixin(value = CraftWorld.class, remap = false)
public abstract class CraftWorldMixin {

    // @formatter:off
    @Shadow @Final private ServerWorld world;
    @Shadow public abstract <T extends Entity> T addEntity(net.minecraft.entity.Entity entity, CreatureSpawnEvent.SpawnReason reason) throws IllegalArgumentException;
    // @formatter:on

    @Inject(method = "spawnEntity", cancellable = true, at = @At("HEAD"))
    private void arclight$useFactory(Location loc, EntityType entityType, CallbackInfoReturnable<Entity> cir) {
        Function<Location, ? extends net.minecraft.entity.Entity> factory = ((EntityTypeBridge) (Object) entityType).bridge$entityFactory();
        if (factory != null) {
            cir.setReturnValue(this.addEntity(factory.apply(loc), CreatureSpawnEvent.SpawnReason.CUSTOM));
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public File getWorldFolder() {
        return ((ServerWorldBridge) this.world).bridge$getConvertable().getDimensionFolder(this.world.getDimensionKey());
    }
}
