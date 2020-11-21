package io.izzel.arclight.common.mixin.core.world;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.world.TeleporterBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.Direction;
import net.minecraft.util.TeleportationRepositioner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Teleporter;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.util.BlockStateListPopulator;
import org.bukkit.event.world.PortalCreateEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(Teleporter.class)
public abstract class TeleporterMixin implements TeleporterBridge {

    // @formatter:off
    @Shadow public abstract Optional<TeleportationRepositioner.Result> makePortal(BlockPos pos, Direction.Axis axis);
    @Shadow @Final protected ServerWorld world;
    @Shadow public abstract Optional<TeleportationRepositioner.Result> getExistingPortal(BlockPos pos, boolean isNether);
    // @formatter:on

    @ModifyVariable(method = "getExistingPortal", index = 4, at = @At(value = "INVOKE", target = "Lnet/minecraft/village/PointOfInterestManager;ensureLoadedAndValid(Lnet/minecraft/world/IWorldReader;Lnet/minecraft/util/math/BlockPos;I)V"))
    private int arclight$useSearchRadius(int i) {
        return this.arclight$searchRadius == -1 ? i : this.arclight$searchRadius;
    }

    private transient int arclight$searchRadius = -1;

    public Optional<TeleportationRepositioner.Result> findPortal(BlockPos pos, int searchRadius) {
        this.arclight$searchRadius = searchRadius;
        try {
            return this.getExistingPortal(pos, false);
        } finally {
            this.arclight$searchRadius = -1;
        }
    }

    @Override
    public Optional<TeleportationRepositioner.Result> bridge$findPortal(BlockPos pos, int searchRadius) {
        return findPortal(pos, searchRadius);
    }

    @ModifyArg(method = "makePortal", index = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;func_243514_a(Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/util/Direction;Lnet/minecraft/util/Direction;)Ljava/lang/Iterable;"))
    private int arclight$changeRadius(int i) {
        return this.arclight$createRadius == -1 ? i : this.arclight$createRadius;
    }

    @Redirect(method = "makePortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    private boolean arclight$captureBlocks1(ServerWorld serverWorld, BlockPos pos, BlockState state) {
        if (this.arclight$populator == null) {
            this.arclight$populator = new BlockStateListPopulator(serverWorld);
        }
        return this.arclight$populator.setBlockState(pos, state, 3);
    }

    @Redirect(method = "makePortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private boolean arclight$captureBlocks2(ServerWorld serverWorld, BlockPos pos, BlockState state, int flags) {
        if (this.arclight$populator == null) {
            this.arclight$populator = new BlockStateListPopulator(serverWorld);
        }
        return this.arclight$populator.setBlockState(pos, state, flags);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "makePortal", cancellable = true, at = @At("RETURN"))
    private void arclight$portalCreate(BlockPos pos, Direction.Axis axis, CallbackInfoReturnable<Optional<TeleportationRepositioner.Result>> cir) {
        CraftWorld craftWorld = ((WorldBridge) this.world).bridge$getWorld();
        List<org.bukkit.block.BlockState> blockStates;
        if (this.arclight$populator == null) {
            blockStates = new ArrayList<>();
        } else {
            blockStates = (List) this.arclight$populator.getList();
        }
        PortalCreateEvent event = new PortalCreateEvent(blockStates, craftWorld, (this.arclight$entity == null) ? null : ((EntityBridge) this.arclight$entity).bridge$getBukkitEntity(), PortalCreateEvent.CreateReason.NETHER_PAIR);

        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(Optional.empty());
            return;
        }
        if (this.arclight$populator != null) {
            this.arclight$populator.updateList();
        }
    }

    private transient BlockStateListPopulator arclight$populator;
    private transient Entity arclight$entity;
    private transient int arclight$createRadius = -1;

    public Optional<TeleportationRepositioner.Result> createPortal(BlockPos pos, Direction.Axis axis, Entity entity, int createRadius) {
        this.arclight$entity = entity;
        this.arclight$createRadius = createRadius;
        try {
            return this.makePortal(pos, axis);
        } finally {
            this.arclight$entity = null;
            this.arclight$createRadius = -1;
        }
    }

    @Override
    public Optional<TeleportationRepositioner.Result> bridge$createPortal(BlockPos pos, Direction.Axis axis, Entity entity, int createRadius) {
        return createPortal(pos, axis, entity, createRadius);
    }
}
