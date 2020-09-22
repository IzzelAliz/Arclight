package io.izzel.arclight.common.mixin.core.entity.passive;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collections;
import java.util.List;

@Mixin(MooshroomEntity.class)
public abstract class MooshroomEntityMixin extends AnimalEntityMixin {

    @SuppressWarnings("UnresolvedMixinReference")
    @Redirect(method = "onSheared", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/MooshroomEntity;func_70106_y()V"))
    private void arclight$animalTransformPre(MooshroomEntity mooshroomEntity) {
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "onSheared", remap = false, cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;func_217376_c(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$animalTransform(PlayerEntity player, ItemStack item, World world, BlockPos pos, int fortune, CallbackInfoReturnable<List<ItemStack>> cir, CowEntity cowEntity) {
        if (CraftEventFactory.callEntityTransformEvent((MooshroomEntity) (Object) this, cowEntity, EntityTransformEvent.TransformReason.SHEARED).isCancelled()) {
            cir.setReturnValue(Collections.emptyList());
        } else {
            ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SHEARED);
            this.remove();
        }
    }
}
