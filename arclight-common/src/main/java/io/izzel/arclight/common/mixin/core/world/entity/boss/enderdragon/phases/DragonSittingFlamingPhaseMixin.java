package io.izzel.arclight.common.mixin.core.world.entity.boss.enderdragon.phases;

import com.destroystokyo.paper.event.entity.EnderDragonFlameEvent;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonSittingPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonSittingFlamingPhase;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonSittingFlamingPhase.class)
public abstract class DragonSittingFlamingPhaseMixin extends AbstractDragonSittingPhase {

    @Shadow @Nullable private AreaEffectCloud flame;
    @Shadow public abstract void end();

    protected DragonSittingFlamingPhaseMixin(EnderDragon dragon) {
        super(dragon);
    }

    @Decorate(method = "doServerTick(Lnet/minecraft/server/level/ServerLevel;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"), require = 1)
    private boolean arclight$flameEvent(ServerLevel level, Entity cloud) throws Throwable {
        var event = new EnderDragonFlameEvent((org.bukkit.entity.EnderDragon) this.dragon.bridge$getBukkitEntity(),
            (org.bukkit.entity.AreaEffectCloud) cloud.bridge$getBukkitEntity());
        if (!event.callEvent()) {
            // Use the native cleanup, including the existing DESPAWN removal cause.
            this.end();
            return false;
        }
        return (boolean) DecorationOps.callsite().invoke(level, cloud);
    }

    @Inject(method = "end", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/AreaEffectCloud;discard()V"))
    private void arclight$despawn(CallbackInfo ci) {
        this.flame.bridge().bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }
}
