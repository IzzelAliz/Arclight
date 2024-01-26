package io.izzel.arclight.common.mixin.core.world.entity.animal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Sniffer.class)
public abstract class SnifferMixin extends AnimalMixin {

    @Redirect(method = "dropSeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$dropSeed(ServerLevel instance, Entity entity) {
        var event = new EntityDropItemEvent(this.getBukkitEntity(), (Item) entity.bridge$getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        return instance.addFreshEntity(entity);
    }
}
