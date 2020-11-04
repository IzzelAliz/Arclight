package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.block.PortalInfoBridge;
import io.izzel.arclight.common.bridge.block.PortalSizeBridge;
import io.izzel.arclight.common.bridge.world.IWorldBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.block.PortalInfo;
import net.minecraft.block.PortalSize;
import net.minecraft.entity.EntitySize;
import net.minecraft.util.Direction;
import net.minecraft.util.TeleportationRepositioner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.event.CraftPortalEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Mixin(PortalSize.class)
public abstract class PortalSizeMixin implements PortalSizeBridge {

    // @formatter:off
    @Shadow @Final private IWorld world;
    @Shadow public abstract void placePortalBlocks();
    @Shadow @Final private Direction.Axis axis;
    @Shadow @Nullable private BlockPos bottomLeft;
    @Shadow private int height;
    @Shadow @Final private Direction rightDir;
    @Shadow private int width;
    @Shadow public static PortalInfo func_242963_a(ServerWorld world, TeleportationRepositioner.Result result, Direction.Axis axis, Vector3d offsetVector, EntitySize size, Vector3d motion, float rotationYaw, float rotationPitch) { return null; }
    // @formatter:on

    List<BlockState> blocks = new ArrayList<>();

    @Redirect(method = "func_242972_a", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/block/AbstractBlock$IPositionPredicate;test(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean arclight$captureBlock(AbstractBlock.IPositionPredicate predicate, net.minecraft.block.BlockState p_test_1_, IBlockReader p_test_2_, BlockPos pos) {
        boolean test = predicate.test(p_test_1_, p_test_2_, pos);
        if (test) {
            blocks.add(CraftBlock.at(this.world, pos).getState());
        }
        return test;
    }

    @Inject(method = "placePortalBlocks", cancellable = true, at = @At("HEAD"))
    private void arclight$buildPortal(CallbackInfo ci) {
        World world = ((WorldBridge) ((IWorldBridge) this.world).bridge$getMinecraftWorld()).bridge$getWorld();
        net.minecraft.block.BlockState blockState = Blocks.NETHER_PORTAL.getDefaultState().with(NetherPortalBlock.AXIS, this.axis);
        BlockPos.getAllInBoxMutable(this.bottomLeft, this.bottomLeft.offset(Direction.UP, this.height - 1).offset(this.rightDir, this.width - 1)).forEach(pos -> {
            CraftBlockState state = CraftBlockState.getBlockState(((IWorldBridge) this.world).bridge$getMinecraftWorld(), pos, 18);
            state.setData(blockState);
            this.blocks.add(state);
        });
        PortalCreateEvent event = new PortalCreateEvent(this.blocks, world, null, PortalCreateEvent.CreateReason.FIRE);
        Bukkit.getPluginManager().callEvent(event);
        arclight$ret = !event.isCancelled();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    private transient boolean arclight$ret;

    public boolean createPortal() {
        this.placePortalBlocks();
        return arclight$ret;
    }

    @Override
    public boolean bridge$createPortal() {
        return createPortal();
    }

    @SuppressWarnings("ConstantConditions")
    @Redirect(method = "func_242963_a", at = @At(value = "NEW", target = "net/minecraft/block/PortalInfo"))
    private static PortalInfo arclight$setPortalInfo(Vector3d pos, Vector3d motion, float rotationYaw, float rotationPitch, ServerWorld world) {
        PortalInfo portalInfo = new PortalInfo(pos, motion, rotationYaw, rotationPitch);
        ((PortalInfoBridge) portalInfo).bridge$setWorld(world);
        ((PortalInfoBridge) portalInfo).bridge$setPortalEventInfo(ArclightCaptures.getCraftPortalEvent());
        return portalInfo;
    }

    private static PortalInfo a(ServerWorld world, TeleportationRepositioner.Result result, Direction.Axis axis, Vector3d offsetVector, EntitySize size, Vector3d motion, float rotationYaw, float rotationPitch, CraftPortalEvent event) {
        ArclightCaptures.captureCraftPortalEvent(event);
        return func_242963_a(world, result, axis, offsetVector, size, motion, rotationYaw, rotationPitch);
    }
}
