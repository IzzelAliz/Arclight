package io.izzel.arclight.common.mixin.v1_15.world;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.world.TeleporterBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.PointOfInterest;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.village.PointOfInterestType;
import net.minecraft.world.Teleporter;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;
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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Mixin(Teleporter.class)
public abstract class TeleporterMixin_1_15 implements TeleporterBridge {

    // @formatter:off
    @Shadow @Final protected ServerWorld world;
    @Shadow @Final protected Random random;
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
        return this.createPortal(entityIn, pos, createRadius);
    }

    public boolean createPortal(Entity entity, BlockPos createPosition, int createRadius) {
        boolean flag = true;
        double d0 = -1.0;
        int i = createPosition.getX();
        int j = createPosition.getY();
        int k = createPosition.getZ();
        int l = i;
        int i2 = j;
        int j2 = k;
        int k2 = 0;
        int l2 = this.random.nextInt(4);
        BlockPos.Mutable blockposition_mutableblockposition = new BlockPos.Mutable();
        for (int i3 = i - createRadius; i3 <= i + createRadius; ++i3) {
            double d2 = i3 + 0.5 - createPosition.getX();
            for (int j3 = k - createRadius; j3 <= k + createRadius; ++j3) {
                double d3 = j3 + 0.5 - createPosition.getZ();
                Label_0439:
                for (int k3 = this.world.getActualHeight() - 1; k3 >= 0; --k3) {
                    if (this.world.isAirBlock(blockposition_mutableblockposition.setPos(i3, k3, j3))) {
                        while (k3 > 0 && this.world.isAirBlock(blockposition_mutableblockposition.setPos(i3, k3 - 1, j3))) {
                            --k3;
                        }
                        for (int i4 = l2; i4 < l2 + 4; ++i4) {
                            int l3 = i4 % 2;
                            int j4 = 1 - l3;
                            if (i4 % 4 >= 2) {
                                l3 = -l3;
                                j4 = -j4;
                            }
                            for (int l4 = 0; l4 < 3; ++l4) {
                                for (int i5 = 0; i5 < 4; ++i5) {
                                    for (int k4 = -1; k4 < 4; ++k4) {
                                        int k5 = i3 + (i5 - 1) * l3 + l4 * j4;
                                        int j5 = k3 + k4;
                                        int l5 = j3 + (i5 - 1) * j4 - l4 * l3;
                                        blockposition_mutableblockposition.setPos(k5, j5, l5);
                                        if (k4 < 0 && !this.world.getBlockState(blockposition_mutableblockposition).getMaterial().isSolid()) {
                                            continue Label_0439;
                                        }
                                        if (k4 >= 0 && !this.world.isAirBlock(blockposition_mutableblockposition)) {
                                            continue Label_0439;
                                        }
                                    }
                                }
                            }
                            double d4 = k3 + 0.5 - entity.getPosY();
                            double d5 = d2 * d2 + d4 * d4 + d3 * d3;
                            if (d0 < 0.0 || d5 < d0) {
                                d0 = d5;
                                l = i3;
                                i2 = k3;
                                j2 = j3;
                                k2 = i4 % 4;
                            }
                        }
                    }
                }
            }
        }
        if (d0 < 0.0) {
            for (int i3 = i - createRadius; i3 <= i + createRadius; ++i3) {
                double d2 = i3 + 0.5 - createPosition.getX();
                for (int j3 = k - createRadius; j3 <= k + createRadius; ++j3) {
                    double d3 = j3 + 0.5 - createPosition.getZ();
                    Label_0812:
                    for (int k3 = this.world.getActualHeight() - 1; k3 >= 0; --k3) {
                        if (this.world.isAirBlock(blockposition_mutableblockposition.setPos(i3, k3, j3))) {
                            while (k3 > 0 && this.world.isAirBlock(blockposition_mutableblockposition.setPos(i3, k3 - 1, j3))) {
                                --k3;
                            }
                            for (int i4 = l2; i4 < l2 + 2; ++i4) {
                                int l3 = i4 % 2;
                                int j4 = 1 - l3;
                                for (int l4 = 0; l4 < 4; ++l4) {
                                    for (int i5 = -1; i5 < 4; ++i5) {
                                        int k4 = i3 + (l4 - 1) * l3;
                                        int k5 = k3 + i5;
                                        int j5 = j3 + (l4 - 1) * j4;
                                        blockposition_mutableblockposition.setPos(k4, k5, j5);
                                        if (i5 < 0 && !this.world.getBlockState(blockposition_mutableblockposition).getMaterial().isSolid()) {
                                            continue Label_0812;
                                        }
                                        if (i5 >= 0 && !this.world.isAirBlock(blockposition_mutableblockposition)) {
                                            continue Label_0812;
                                        }
                                    }
                                }
                                double d4 = k3 + 0.5 - entity.getPosY();
                                double d5 = d2 * d2 + d4 * d4 + d3 * d3;
                                if (d0 < 0.0 || d5 < d0) {
                                    d0 = d5;
                                    l = i3;
                                    i2 = k3;
                                    j2 = j3;
                                    k2 = i4 % 2;
                                }
                            }
                        }
                    }
                }
            }
        }
        int i6 = l;
        int j6 = i2;
        int j3 = j2;
        int k6 = k2 % 2;
        int l6 = 1 - k6;
        if (k2 % 4 >= 2) {
            k6 = -k6;
            l6 = -l6;
        }
        BlockStateListPopulator blockList = new BlockStateListPopulator(this.world);
        if (d0 < 0.0) {
            i2 = (j6 = MathHelper.clamp(i2, 70, this.world.getActualHeight() - 10));
            for (int k3 = -1; k3 <= 1; ++k3) {
                for (int i4 = 1; i4 < 3; ++i4) {
                    for (int l3 = -1; l3 < 3; ++l3) {
                        int j4 = i6 + (i4 - 1) * k6 + k3 * l6;
                        int l4 = j6 + l3;
                        int i5 = j3 + (i4 - 1) * l6 - k3 * k6;
                        boolean flag2 = l3 < 0;
                        blockposition_mutableblockposition.setPos(j4, l4, i5);
                        blockList.setBlockState(blockposition_mutableblockposition, flag2 ? Blocks.OBSIDIAN.getDefaultState() : Blocks.AIR.getDefaultState(), 3);
                    }
                }
            }
        }
        for (int k3 = -1; k3 < 3; ++k3) {
            for (int i4 = -1; i4 < 4; ++i4) {
                if (k3 == -1 || k3 == 2 || i4 == -1 || i4 == 3) {
                    blockposition_mutableblockposition.setPos(i6 + k3 * k6, j6 + i4, j3 + k3 * l6);
                    blockList.setBlockState(blockposition_mutableblockposition, Blocks.OBSIDIAN.getDefaultState(), 3);
                }
            }
        }
        BlockState iblockdata = (Blocks.NETHER_PORTAL.getDefaultState()).with(NetherPortalBlock.AXIS, (k6 == 0) ? Direction.Axis.Z : Direction.Axis.X);
        for (int i4 = 0; i4 < 2; ++i4) {
            for (int l3 = 0; l3 < 3; ++l3) {
                blockposition_mutableblockposition.setPos(i6 + i4 * k6, j6 + l3, j3 + i4 * l6);
                blockList.setBlockState(blockposition_mutableblockposition, iblockdata, 18);
            }
        }
        org.bukkit.World bworld = ((WorldBridge) this.world).bridge$getWorld();
        PortalCreateEvent event = new PortalCreateEvent((List) blockList.getList(), bworld, ((EntityBridge) entity).bridge$getBukkitEntity(), PortalCreateEvent.CreateReason.NETHER_PAIR);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            blockList.updateList();
        }
        return true;
    }

    public BlockPattern.PortalInfo findAndTeleport(Entity p_222268_1_, BlockPos pos, float p_222268_2_, int searchRadius, boolean searchOnly) {
        Vec3d vec3d = p_222268_1_.getLastPortalVec();
        Direction direction = p_222268_1_.getTeleportDirection();
        BlockPattern.PortalInfo portalInfo = this.findPortal(new BlockPos(p_222268_1_), p_222268_1_.getMotion(), direction, vec3d.x, vec3d.y, p_222268_1_ instanceof PlayerEntity, searchRadius);
        if (searchOnly) return portalInfo;
        if (portalInfo == null) {
            return null;
        } else {
            Vec3d vec3d1 = portalInfo.pos;
            Vec3d vec3d2 = portalInfo.motion;
            p_222268_1_.setMotion(vec3d2);
            p_222268_1_.rotationYaw = p_222268_2_ + (float) portalInfo.rotation;
            p_222268_1_.moveForced(vec3d1.x, vec3d1.y, vec3d1.z);
            return portalInfo;
        }
    }

    @Override
    public BlockPattern.PortalInfo bridge$placeInPortal(Entity p_222268_1_, BlockPos pos, float p_222268_2_, int searchRadius, boolean searchOnly) {
        return findAndTeleport(p_222268_1_, pos, p_222268_2_, searchRadius, searchOnly);
    }

    public BlockPattern.PortalInfo findPortal(BlockPos p_222272_1_, Vec3d p_222272_2_, Direction directionIn, double p_222272_4_, double p_222272_6_, boolean p_222272_8_, int searchRadius) {
        PointOfInterestManager pointofinterestmanager = this.world.getPointOfInterestManager();
        pointofinterestmanager.ensureLoadedAndValid(this.world, p_222272_1_, 128);
        List<PointOfInterest> list = pointofinterestmanager.getInSquare((p_226705_0_) -> {
            return p_226705_0_ == PointOfInterestType.NETHER_PORTAL;
        }, p_222272_1_, searchRadius, PointOfInterestManager.Status.ANY).collect(Collectors.toList());
        Optional<PointOfInterest> optional = list.stream().min(Comparator.<PointOfInterest>comparingDouble((p_226706_1_) -> {
            return p_226706_1_.getPos().distanceSq(p_222272_1_);
        }).thenComparingInt((p_226704_0_) -> {
            return p_226704_0_.getPos().getY();
        }));
        return optional.map((p_226707_7_) -> {
            BlockPos blockpos = p_226707_7_.getPos();
            this.world.getChunkProvider().registerTicket(TicketType.PORTAL, new ChunkPos(blockpos), 3, blockpos);
            BlockPattern.PatternHelper blockpattern$patternhelper = NetherPortalBlock.createPatternHelper(this.world, blockpos);
            return blockpattern$patternhelper.getPortalInfo(directionIn, blockpos, p_222272_6_, p_222272_2_, p_222272_4_);
        }).orElse(null);
    }
}
