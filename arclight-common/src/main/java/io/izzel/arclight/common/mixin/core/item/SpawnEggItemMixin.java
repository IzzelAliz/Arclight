package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(SpawnEggItem.class)
public class SpawnEggItemMixin {

    @Inject(method = "func_234809_a_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;func_242417_l(Lnet/minecraft/entity/Entity;)V"))
    private void arclight$reason(PlayerEntity p_234809_1_, MobEntity p_234809_2_, EntityType<? extends MobEntity> p_234809_3_, ServerWorld world, Vector3d p_234809_5_, ItemStack p_234809_6_, CallbackInfoReturnable<Optional<MobEntity>> cir) {
        ((WorldBridge) world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);
    }
}
