package io.izzel.arclight.common.mixin.core.world.entity.monster;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import io.izzel.arclight.common.mixin.core.world.entity.AgeableMobMixin;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory.SlimeSplit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.entity.monster.cubemob.AbstractCubeMob.class)
public abstract class AbstractCubeMobMixin extends AgeableMobMixin {
    @WrapMethod(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V")
    private void arclight$splitScope(Entity.RemovalReason reason, Operation<Void> original) {
        var previous = io.izzel.arclight.common.mod.util.ArclightCaptures.slimeSplit;
        io.izzel.arclight.common.mod.util.ArclightCaptures.slimeSplit = new SlimeSplit((Mob) (Object) this);
        try {
            original.call(reason);
        } finally {
            io.izzel.arclight.common.mod.util.ArclightCaptures.slimeSplit = previous;
        }
    }

    @Inject(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/cubemob/AbstractCubeMob;getTeam()Lnet/minecraft/world/scores/PlayerTeam;", shift = At.Shift.AFTER),
        cancellable = true, require = 1)
    private void arclight$splitCount(Entity.RemovalReason reason, CallbackInfo ci, @Local(index = 6) LocalIntRef count) {
        SlimeSplitEvent event = new SlimeSplitEvent((org.bukkit.entity.AbstractCubeMob) this.getBukkitEntity(), count.get());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getCount() <= 0) {
            super.remove(reason);
            ci.cancel();
        } else {
            count.set(event.getCount());
        }
    }
}
