package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.mod.server.entity.EntityClassLookup;
import net.minecraft.world.entity.Entity;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftEntity.class, remap = false)
public abstract class CraftEntityMixin implements org.bukkit.entity.Entity {

    @Shadow protected Entity entity;
    @Shadow @Final protected CraftServer server;

    // @Inject(method = "getEntity", cancellable = true, at = @At("HEAD"))
    // private static void arclight$fakePlayer(CraftServer server, Entity entity, CallbackInfoReturnable<CraftEntity> cir) {
    //     if (entity instanceof FakePlayer) {
    //         cir.setReturnValue(new ArclightFakePlayer(server, (FakePlayer) entity));
    //     }
    // }

    @Inject(method = "getEntity", cancellable = true, at = @At(value = "NEW", target = "java/lang/AssertionError"))
    private static void arclight$modEntity(CraftServer server, Entity entity, CallbackInfoReturnable<CraftEntity> cir) {
        var craftEntity = (CraftEntity) EntityClassLookup.getEntityTypeData(entity).convertFunction().apply(server, entity);
        cir.setReturnValue(craftEntity);
    }
}
