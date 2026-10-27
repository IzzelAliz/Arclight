package io.izzel.arclight.common.mixin.core.server.level;

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerSpectatingEventMixin {

    @Shadow private Entity camera;

    // This call is reached only after a changed camera has been assigned. On
    // NeoForge it is also after PartEntity has been resolved to its parent.
    @Decorate(method = "setCamera(Lnet/minecraft/world/entity/Entity;)V", inject = true, require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;level()Lnet/minecraft/world/level/Level;"))
    private void arclight$spectatingEvent(Entity newCamera, @Local(ordinal = 1) Entity oldCamera) throws Throwable {
        var player = ((ServerPlayerBridge) (Object) this).bridge$getBukkitEntity();
        // Match the pinned Paper producer: both constructors receive the
        // already-assigned camera, not the saved oldCamera local.
        boolean allowed = this.camera == (Object) this
            ? new PlayerStopSpectatingEntityEvent(player, ((EntityBridge) this.camera).bridge$getBukkitEntity()).callEvent()
            : new PlayerStartSpectatingEntityEvent(player, ((EntityBridge) this.camera).bridge$getBukkitEntity(),
                ((EntityBridge) newCamera).bridge$getBukkitEntity()).callEvent();
        if (!allowed) {
            this.camera = oldCamera;
            DecorationOps.cancel().invoke();
            return;
        }
    }
}
