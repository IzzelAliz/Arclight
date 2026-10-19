package io.izzel.arclight.common.mixin.core.world.entity.monster;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import io.izzel.arclight.common.mixin.core.world.entity.MobMixin;
import io.izzel.arclight.common.mod.server.entity.SlimeSplitScope;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.entity.monster.Slime.class)
public abstract class SlimeMixin extends MobMixin {
    @WrapMethod(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V")
    private void arclight$splitScope(Entity.RemovalReason reason, Operation<Void> original) {
        try (var scope = SlimeSplitScope.enter((Mob) (Object) this)) {
            original.call(reason);
        }
    }

    @Inject(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Slime;getTeam()Lnet/minecraft/world/scores/PlayerTeam;", shift = At.Shift.AFTER),
        cancellable = true, require = 1)
    private void arclight$splitCount(Entity.RemovalReason reason, CallbackInfo ci, @Local(index = 6) LocalIntRef count) {
        SlimeSplitEvent event = new SlimeSplitEvent((org.bukkit.entity.Slime) this.getBukkitEntity(), count.get());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getCount() <= 0) {
            super.remove(reason);
            ci.cancel();
        } else {
            count.set(event.getCount());
        }
    }
}
