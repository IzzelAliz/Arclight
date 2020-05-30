package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.block.BlockState;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {

    @Inject(method = "onEntityCollision", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;changeDimension(Lnet/minecraft/world/dimension/DimensionType;)Lnet/minecraft/entity/Entity;"))
    public void arclight$enterPortal(BlockState state, World worldIn, BlockPos pos, Entity entityIn, CallbackInfo ci) {
        EntityPortalEnterEvent event = new EntityPortalEnterEvent(((EntityBridge) entityIn).bridge$getBukkitEntity(),
            new Location(((WorldBridge) worldIn).bridge$getWorld(), pos.getX(), pos.getY(), pos.getZ()));
        Bukkit.getPluginManager().callEvent(event);
        if (entityIn instanceof ServerPlayerEntity) {
            ((ServerPlayerEntityBridge) entityIn).bridge$changeDimension(worldIn.dimension.getType() == DimensionType.THE_END ? DimensionType.OVERWORLD : DimensionType.THE_END,
                PlayerTeleportEvent.TeleportCause.END_PORTAL);
            ci.cancel();
        }
    }
}
