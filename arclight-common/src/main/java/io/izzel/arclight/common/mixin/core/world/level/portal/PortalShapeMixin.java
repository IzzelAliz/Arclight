package io.izzel.arclight.common.mixin.core.world.level.portal;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.bridge.core.world.level.LevelAccessorBridge;
import io.izzel.arclight.common.bridge.core.world.level.portal.PortalShapeBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v.util.BlockStateListPopulator;
import org.bukkit.event.world.PortalCreateEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

@Mixin(PortalShape.class)
public abstract class PortalShapeMixin implements PortalShapeBridge {

    // Actual 1.21.11 PortalShape fields; it has no LevelAccessor field.
    // @formatter:off
    @Shadow @Final private Direction.Axis axis;
    @Shadow @Final private Direction rightDir;
    @Shadow @Final private BlockPos bottomLeft;
    @Shadow @Final private int height;
    @Shadow @Final private int width;
    // @formatter:on

    @Unique private static final ThreadLocal<Deque<Optional<BlockStateListPopulator>>> arclight$portalScanScopes = new ThreadLocal<>();
    @Unique private @Nullable BlockStateListPopulator arclight$portalBlocks;

    @WrapMethod(method = "findAnyShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction$Axis;)Lnet/minecraft/world/level/portal/PortalShape;")
    private static PortalShape arclight$capturePortalShape(BlockGetter level, BlockPos pos, Direction.Axis axis, Operation<PortalShape> original) {
        boolean actual = level instanceof LevelAccessor levelAccessor
            && levelAccessor instanceof LevelAccessorBridge bridge
            && bridge.arclight$isActual();
        Deque<Optional<BlockStateListPopulator>> scopes = arclight$portalScanScopes.get();
        if (!actual && (scopes == null || scopes.isEmpty())) {
            return original.call(level, pos, axis);
        }
        if (scopes == null) {
            scopes = new ArrayDeque<>();
            arclight$portalScanScopes.set(scopes);
        }
        scopes.push(actual ? Optional.of(new BlockStateListPopulator((LevelAccessor) level)) : Optional.empty());
        try {
            PortalShape shape = original.call(level, pos, axis);
            BlockStateListPopulator blocks = scopes.peek().orElse(null);
            if (blocks != null) {
                ((PortalShapeBridge) (Object) shape).bridge$setPortalBlocks(blocks);
            }
            return shape;
        } finally {
            scopes.pop();
            if (scopes.isEmpty()) {
                arclight$portalScanScopes.remove();
            }
        }
    }

    /** Preserve the actual FRAME predicate, including NeoForge custom frames. */
    @Decorate(method = {"getDistanceUntilEdgeAboveFrame", "hasTopFrame", "getDistanceUntilTop"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockBehaviour$StatePredicate;test(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean arclight$capturePortalFrame(BlockBehaviour.StatePredicate frame, BlockState state, BlockGetter level, BlockPos pos) throws Throwable {
        boolean matches = (boolean) DecorationOps.callsite().invoke(frame, state, level, pos);
        Deque<Optional<BlockStateListPopulator>> scopes = arclight$portalScanScopes.get();
        BlockStateListPopulator blocks = scopes == null || scopes.isEmpty() ? null : scopes.peek().orElse(null);
        if (matches && blocks != null) {
            blocks.setBlock(pos, state, 18);
        }
        return matches;
    }

    @WrapMethod(method = "createPortalBlocks(Lnet/minecraft/world/level/LevelAccessor;)V")
    private void arclight$createPortalBlocks(LevelAccessor level, Operation<Void> original) {
        if (!(level instanceof LevelAccessorBridge bridge) || !bridge.arclight$isActual()) {
            original.call(level);
            return;
        }
        BlockStateListPopulator blocks = this.arclight$portalBlocks == null ? new BlockStateListPopulator(level) : this.arclight$portalBlocks;
        World world = bridge.bridge$getMinecraftWorld().bridge$getWorld();
        BlockState portal = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, this.axis);
        BlockPos.betweenClosed(this.bottomLeft, this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1))
            .forEach(pos -> blocks.setBlock(pos, portal, 18));
        PortalCreateEvent event = new PortalCreateEvent((java.util.List<org.bukkit.block.BlockState>) (java.util.List) blocks.getList(), world, null, PortalCreateEvent.CreateReason.FIRE);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            original.call(level);
        }
    }

    @Override
    public void bridge$setPortalBlocks(BlockStateListPopulator blocks) {
        this.arclight$portalBlocks = blocks;
    }

}
