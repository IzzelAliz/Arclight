package io.izzel.arclight.common.mixin.core.world.entity.monster;

import com.destroystokyo.paper.event.entity.SlimeChangeDirectionEvent;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import org.bukkit.Bukkit;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.monster.cubemob.AbstractCubeMob$CubeMobRandomDirectionGoal")
public abstract class SlimeRandomDirectionGoalMixin {

    @Shadow @Final private AbstractCubeMob cubeMob;
    @Shadow private float chosenDegrees;

    @Decorate(method = "tick()V", inject = true,
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/monster/cubemob/AbstractCubeMob$CubeMobRandomDirectionGoal;chosenDegrees:F",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER),
        require = 1, expect = 1, allow = 1)
    private void arclight$changeDirectionEvent() throws Throwable {
        // Paper keeps the random yaw/timer writes, even when this tick is vetoed.
        SlimeChangeDirectionEvent event = new SlimeChangeDirectionEvent(
            (org.bukkit.entity.AbstractCubeMob) ((EntityBridge) this.cubeMob).bridge$getBukkitEntity(), this.chosenDegrees);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke();
            return;
        }
        this.chosenDegrees = event.getNewYaw();
    }
}
