package io.izzel.arclight.common.mixin.core.entity.passive;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(MooshroomEntity.class)
public abstract class MooshroomEntityMixin extends AnimalEntityMixin {

    @Redirect(method = "onSheared", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/MooshroomEntity;remove()V"))
    private void arclight$animalTransformPre(MooshroomEntity mooshroomEntity) {
    }

    @Inject(method = "onSheared", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$animalTransform(ItemStack item, IWorld world, BlockPos pos, int fortune, CallbackInfoReturnable<List<ItemStack>> cir, List<ItemStack> stackList, CowEntity cowEntity) {
        if (CraftEventFactory.callEntityTransformEvent((MooshroomEntity) (Object) this, cowEntity, EntityTransformEvent.TransformReason.SHEARED).isCancelled()) {
            cir.setReturnValue(stackList);
        } else {
            ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SHEARED);
            this.remove();
        }
    }
}
