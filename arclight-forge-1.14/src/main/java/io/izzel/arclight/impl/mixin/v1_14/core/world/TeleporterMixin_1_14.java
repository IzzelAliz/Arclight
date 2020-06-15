package io.izzel.arclight.impl.mixin.v1_14.core.world;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.world.TeleporterBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Teleporter;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.util.BlockStateListPopulator;
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
public abstract class TeleporterMixin_1_14 implements TeleporterBridge {

    // @formatter:off
    @Shadow @Final protected ServerWorld world;
    @Shadow public abstract boolean makePortal(Entity entityIn);
    // @formatter:on

    private transient BlockStateListPopulator arclight$populator;

    @Redirect(method = "makePortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public boolean arclight$portalPlace1(ServerWorld serverWorld, BlockPos pos, BlockState newState, int flags) {
        if (arclight$populator == null) {
            arclight$populator = new BlockStateListPopulator(serverWorld);
        }
        return arclight$populator.setBlockState(pos, newState, flags);
    }

    @Redirect(method = "makePortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    public boolean arclight$portalPlace2(ServerWorld serverWorld, BlockPos pos, BlockState state) {
        if (arclight$populator == null) {
            arclight$populator = new BlockStateListPopulator(serverWorld);
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

    @Override
    public boolean bridge$makePortal(Entity entityIn, BlockPos pos, int createRadius) {
        return this.makePortal(entityIn);
    }

    @Override
    public BlockPattern.PortalInfo bridge$placeInPortal(Entity p_222268_1_, BlockPos pos, float p_222268_2_, int searchRadius, boolean searchOnly) {
        throw new IllegalStateException("Not implement");
    }
}
