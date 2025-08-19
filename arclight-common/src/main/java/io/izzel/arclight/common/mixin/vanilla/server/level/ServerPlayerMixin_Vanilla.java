package io.izzel.arclight.common.mixin.vanilla.server.level;

import io.izzel.arclight.common.mixin.vanilla.world.entity.player.PlayerMixin_Vanilla;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin_Vanilla extends PlayerMixin_Vanilla {

    @Decorate(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$capturePlayerDrop(Level instance, Entity entity) throws Throwable {
        if (!bridge$isForceDrops() && this.arclight$captureDrop((ItemEntity) entity)) {
            return true;
        }
        return (boolean) DecorationOps.callsite().invoke(instance, entity);
    }
}