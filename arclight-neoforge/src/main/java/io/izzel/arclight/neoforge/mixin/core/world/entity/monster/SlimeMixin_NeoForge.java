package io.izzel.arclight.neoforge.mixin.core.world.entity.monster;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory.SlimeSplit;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Slime;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import org.spongepowered.asm.mixin.injection.At;
import java.util.ArrayList;
import java.util.function.Consumer;

@Mixin(Slime.class)
public class SlimeMixin_NeoForge {
    @WrapMethod(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V")
    private void arclight$restoreConversionFlag(Entity.RemovalReason reason, Operation<Void> original) {
        MobBridge source = (MobBridge) (Object) this;
        boolean previous = source.bridge$neoforge$preventConversionSpawns();
        try { original.call(reason); }
        finally { source.bridge$neoforge$setPreventConversionSpawns(previous); }
    }

    @WrapOperation(method = "remove", at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z"), require = 1)
    private boolean arclight$nonNullChild(ArrayList<Mob> children, Object child, Operation<Boolean> original) {
        return child != null && original.call(children, child);
    }

    @WrapOperation(method = "remove", at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;forEach(Ljava/util/function/Consumer;)V"), require = 1)
    private void arclight$finalCandidates(ArrayList<Mob> children, Consumer<Mob> admission, Operation<Void> original) {
        Mob source = (Mob) (Object) this;
        if (!SlimeSplit.allow(source, children)) return;
        original.call(children, (Consumer<Mob>) child -> {
            ((WorldBridge) source.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SLIME_SPLIT);
            try { admission.accept(child); }
            finally { ((WorldBridge) source.level()).bridge$pushAddEntityReason(null); }
        });
    }
}
