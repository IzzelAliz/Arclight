package io.izzel.arclight.common.mixin.core.world;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mod.util.ArclightBlockPopulator;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Teleporter;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.event.world.PortalCreateEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Teleporter.class)
public class TeleporterMixin {

    // @formatter:off
    @Shadow @Final protected ServerWorld world;
    // @formatter:on

    private transient ArclightBlockPopulator arclight$populator;

    @Redirect(method = "makePortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public boolean arclight$portalPlace1(ServerWorld serverWorld, BlockPos pos, BlockState newState, int flags) {
        if (arclight$populator == null) {
            arclight$populator = new ArclightBlockPopulator(serverWorld);
        }
        return arclight$populator.setBlockState(pos, newState, flags);
    }

    @Redirect(method = "makePortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    public boolean arclight$portalPlace2(ServerWorld serverWorld, BlockPos pos, BlockState state) {
        if (arclight$populator == null) {
            arclight$populator = new ArclightBlockPopulator(serverWorld);
        }
        return arclight$populator.setBlockState(pos, state, 3);
    }

    @Inject(method = "makePortal", at = @At("RETURN"))
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void arclight$portalCreate(Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        PortalCreateEvent event = new PortalCreateEvent((List) arclight$populator.getList(), ((WorldBridge) this.world).bridge$getWorld(),
            ((EntityBridge) entityIn).bridge$getBukkitEntity(), PortalCreateEvent.CreateReason.NETHER_PAIR);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            arclight$populator.updateList();
        }
        arclight$populator = null;
    }
}
