package io.izzel.arclight.common.mixin.vanilla.world.entity.monster;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory.SlimeSplit;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slime.class)
public class SlimeMixin_Vanilla {
    @WrapOperation(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Slime;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"), require = 1)
    private <T extends Mob> T arclight$collect(Slime source, EntityType<T> type, ConversionParams params,
            EntitySpawnReason reason, ConversionParams.AfterConversion<T> after, Operation<T> original) {
        var scope = SlimeSplit.current(source);
        if (scope == null) return original.call(source, type, params, reason, after);
        var conversion = scope.conversion(after);
        T result = original.call(source, type, params, reason, conversion);
        conversion.complete(result);
        return result;
    }
    @Inject(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"), require = 1)
    private void arclight$commit(Entity.RemovalReason reason, CallbackInfo ci) {
        var scope = SlimeSplit.current((Mob) (Object) this);
        if (scope != null) scope.finish();
    }
}
