package io.izzel.arclight.impl.mixin.v1_14.core.entity.monster;

import io.izzel.arclight.impl.mixin.v1_14.core.entity.MobEntityMixin_1_14;
import net.minecraft.entity.monster.ShulkerEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerEntity.class)
public abstract class ShulkerEntityMixin_1_14 extends MobEntityMixin_1_14 {

    @Inject(method = "notifyDataManagerChange", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/monster/ShulkerEntity;prevPosX:D"))
    private void arclight$chunkCheck(DataParameter<?> key, CallbackInfo ci) {
        if (this.bridge$isValid()) ((ServerWorld) this.world).chunkCheck((ShulkerEntity) (Object) this);
    }
}
