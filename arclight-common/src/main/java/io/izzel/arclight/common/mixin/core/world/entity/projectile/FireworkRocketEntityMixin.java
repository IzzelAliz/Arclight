package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.mixin.core.world.entity.EntityMixin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin extends EntityMixin {

    @Shadow public int life;
    @Shadow public int lifetime;
    @Shadow private void explode(ServerLevel level) { }

    @Override
    public void bridge$inactiveTick() {
        ++this.life;
        if (this.life > this.lifetime && this.level() instanceof ServerLevel serverLevel) {
            // The existing explode injection owns the event and removal cause.
            this.explode(serverLevel);
        }
        super.bridge$inactiveTick();
    }

    @Inject(method = "explode", cancellable = true, at = @At("HEAD"))
    private void arclight$fireworksExplode(CallbackInfo ci) {
        if (!CraftEventFactory.callFireworkExplodeEvent((FireworkRocketEntity) (Object) this)) {
            ci.cancel();
        } else {
            this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.EXPLODE);
        }
    }
}
