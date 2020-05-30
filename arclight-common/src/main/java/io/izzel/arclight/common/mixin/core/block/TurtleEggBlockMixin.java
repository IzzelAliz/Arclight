package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import net.minecraft.block.TurtleEggBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TurtleEggBlock.class)
public class TurtleEggBlockMixin {

    @Inject(method = "tryTrample", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/block/TurtleEggBlock;removeOneEgg(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"))
    public void arclight$stepOn(World world, BlockPos pos, Entity entity, int i, CallbackInfo ci) {
        Cancellable cancellable;
        if (entity instanceof PlayerEntity) {
            cancellable = CraftEventFactory.callPlayerInteractEvent((PlayerEntity) entity, Action.PHYSICAL, pos, null, null, null);
        } else {
            cancellable = new EntityInteractEvent(((EntityBridge) entity).bridge$getBukkitEntity(), CraftBlock.at(world, pos));
            Bukkit.getPluginManager().callEvent((EntityInteractEvent) cancellable);
        }

        if (cancellable.isCancelled()) {
            ci.cancel();
        }
    }
}
