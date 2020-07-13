package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.EntityTypeBridge;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.world.storage.SaveHandlerBridge;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.util.UUID;
import java.util.function.Function;

@Mixin(value = CraftWorld.class, remap = false)
public class CraftWorldMixin {

    // @formatter:off
    @Shadow @Final private ServerWorld world;
    // @formatter:on

    @Inject(method = "spawnEntity", cancellable = true, at = @At("HEAD"))
    private void arclight$useFactory(Location loc, EntityType entityType, CallbackInfoReturnable<Entity> cir) {
        Function<Location, ? extends net.minecraft.entity.Entity> factory = ((EntityTypeBridge) (Object) entityType).bridge$entityFactory();
        if (factory != null) {
            cir.setReturnValue(((EntityBridge) factory.apply(loc)).bridge$getBukkitEntity());
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public UUID getUID() {
        return ((SaveHandlerBridge) this.world.getSaveHandler()).bridge$getUUID(this.world);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public File getWorldFolder() {
        return this.world.dimension.getType().getDirectory(this.world.getSaveHandler().getWorldDirectory());
    }
}
