package io.izzel.arclight.common.mixin.core.world.level.gameevent;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftGameEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameEventDispatcher.class)
public class GameEventDispatcherMixin {

    @Shadow @Final private ServerLevel level;

    @Decorate(method = "post", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;containing(Lnet/minecraft/core/Position;)Lnet/minecraft/core/BlockPos;"))
    private void arclight$gameEvent(Holder<GameEvent> holder, Vec3 vec3, GameEvent.Context context, @Local(ordinal = 0) int i) throws Throwable {
        var entity = context.sourceEntity();
        GenericGameEvent event = new GenericGameEvent(CraftGameEvent.minecraftToBukkit(holder.value()),
            new Location(this.level.bridge$getWorld(), vec3.x(), vec3.y(), vec3.z()), (entity == null) ? null : entity.bridge$getBukkitEntity(), i, !Bukkit.isPrimaryThread());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke();
            return;
        }
        i = event.getRadius();
        DecorationOps.blackhole().invoke(i);
    }
}
