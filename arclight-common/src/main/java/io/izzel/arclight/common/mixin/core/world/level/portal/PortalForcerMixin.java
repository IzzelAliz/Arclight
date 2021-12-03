package io.izzel.arclight.common.mixin.core.world.level.portal;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.TeleporterBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.PortalForcer;
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

@Mixin(PortalForcer.class)
public abstract class PortalForcerMixin implements TeleporterBridge {

    // @formatter:off
    @Shadow public abstract Optional<BlockUtil.FoundRectangle> createPortal(BlockPos pos, Direction.Axis axis);
    @Shadow @Final protected ServerLevel level;
    @Shadow public abstract Optional<BlockUtil.FoundRectangle> findPortalAround(BlockPos p_192986_, boolean p_192987_, WorldBorder p_192988_);
    // @formatter:on

    @ModifyVariable(method = "findPortalAround", index = 5, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;ensureLoadedAndValid(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;I)V"))
    private int arclight$useSearchRadius(int i) {
        return this.arclight$searchRadius == -1 ? i : this.arclight$searchRadius;
    }

    private transient int arclight$searchRadius = -1;

    public Optional<BlockUtil.FoundRectangle> findPortalAround(BlockPos pos, WorldBorder worldBorder, int searchRadius) {
        this.arclight$searchRadius = searchRadius;
        try {
            return this.findPortalAround(pos, false, worldBorder);
        } finally {
            this.arclight$searchRadius = -1;
        }
    }

    @Override
    public Optional<BlockUtil.FoundRectangle> bridge$findPortal(BlockPos pos, WorldBorder worldborder, int searchRadius) {
        return findPortalAround(pos, worldborder, searchRadius);
    }

    @ModifyArg(method = "createPortal", index = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;spiralAround(Lnet/minecraft/core/BlockPos;ILnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;)Ljava/lang/Iterable;"))
    private int arclight$changeRadius(int i) {
        return this.arclight$createRadius == -1 ? i : this.arclight$createRadius;
    }

    @Redirect(method = "createPortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean arclight$captureBlocks1(ServerLevel serverWorld, BlockPos pos, BlockState state) {
        if (this.arclight$populator == null) {
            this.arclight$populator = new BlockStateListPopulator(serverWorld);
        }
        return this.arclight$populator.setBlock(pos, state, 3);
    }

    @Redirect(method = "createPortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean arclight$captureBlocks2(ServerLevel serverWorld, BlockPos pos, BlockState state, int flags) {
        if (this.arclight$populator == null) {
            this.arclight$populator = new BlockStateListPopulator(serverWorld);
        }
        return this.arclight$populator.setBlock(pos, state, flags);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "createPortal", cancellable = true, at = @At("RETURN"))
    private void arclight$portalCreate(BlockPos pos, Direction.Axis axis, CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> cir) {
        CraftWorld craftWorld = ((WorldBridge) this.level).bridge$getWorld();
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

    public Optional<BlockUtil.FoundRectangle> createPortal(BlockPos pos, Direction.Axis axis, Entity entity, int createRadius) {
        this.arclight$entity = entity;
        this.arclight$createRadius = createRadius;
        try {
            return this.createPortal(pos, axis);
        } finally {
            this.arclight$entity = null;
            this.arclight$createRadius = -1;
        }
    }

    @Override
    public Optional<BlockUtil.FoundRectangle> bridge$createPortal(BlockPos pos, Direction.Axis axis, Entity entity, int createRadius) {
        return createPortal(pos, axis, entity, createRadius);
    }
}
