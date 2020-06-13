package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.block.NetherPortalBlockBridge;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.block.BlockState;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {

    @Inject(method = "trySpawnPortal", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/block/NetherPortalBlock$Size;placePortalBlocks()V"))
    public void arclight$spawnPortal(IWorld worldIn, BlockPos pos, CallbackInfoReturnable<Boolean> cir, NetherPortalBlock.Size size) {
        cir.setReturnValue(((NetherPortalBlockBridge.SizeBridge) size).bridge$createPortal());
    }

    @Inject(method = "onEntityCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setPortal(Lnet/minecraft/util/math/BlockPos;)V"))
    public void arclight$portalEnter(BlockState state, World worldIn, BlockPos pos, Entity entityIn, CallbackInfo ci) {
        EntityPortalEnterEvent event = new EntityPortalEnterEvent(((EntityBridge) entityIn).bridge$getBukkitEntity(),
            new Location(((WorldBridge) worldIn).bridge$getWorld(), pos.getX(), pos.getY(), pos.getZ()));
        Bukkit.getPluginManager().callEvent(event);
    }
}
