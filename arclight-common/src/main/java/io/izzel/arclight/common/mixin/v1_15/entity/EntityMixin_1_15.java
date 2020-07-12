package io.izzel.arclight.common.mixin.v1_15.entity;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.ITeleporter;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Random;

@Mixin(Entity.class)
public abstract class EntityMixin_1_15 implements EntityBridge {

    // @formatter:off
    @Shadow public World world;
    @Shadow @Deprecated public boolean removed;
    @Shadow @Nullable public abstract MinecraftServer getServer();
    @Shadow public DimensionType dimension;
    @Shadow public abstract void detach();
    @Shadow public float rotationYaw;
    @Shadow public float rotationPitch;
    @Shadow public abstract void setMotion(Vec3d motionIn);
    @Shadow(remap = false) public abstract void remove(boolean keepData);
    @Shadow public abstract Vec3d getMotion();
    @Shadow public abstract double getPosX();
    @Shadow public abstract double getPosZ();
    @Shadow public abstract Vec3d getLastPortalVec();
    @Shadow public abstract double getPosY();
    @Shadow public abstract Direction getTeleportDirection();
    @Shadow public abstract EntityType<?> getType();
    @Shadow @Final protected EntityDataManager dataManager;
    @Shadow public abstract boolean isInvisible();
    @Shadow @Final protected Random rand;
    @Shadow public abstract float getWidth();
    @Shadow public abstract float getHeight();
    @Shadow public abstract double getPosYEye();
    @Shadow public abstract void setFlag(int flag, boolean set);
    @Shadow public abstract Vec3d getPositionVec();
    @Shadow(remap = false) public abstract void revive();
    @Shadow public abstract void setWorld(World worldIn);
    @Shadow public abstract int getEntityId();
    @Shadow @Nullable public abstract Entity changeDimension(DimensionType destination);
    @Shadow public boolean collidedHorizontally;
    @Shadow protected abstract Vec3d getAllowedMovement(Vec3d vec);
    // @formatter:on

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;canTriggerWalking()Z"))
    private void arclight$move$blockCollide(MoverType typeIn, Vec3d pos, CallbackInfo ci) {
        if (collidedHorizontally && this.bridge$getBukkitEntity() instanceof Vehicle) {
            Vehicle vehicle = (Vehicle) this.bridge$getBukkitEntity();
            org.bukkit.block.Block block = ((WorldBridge) this.world).bridge$getWorld().getBlockAt(MathHelper.floor(this.getPosX()), MathHelper.floor(this.getPosY()), MathHelper.floor(this.getPosZ()));
            Vec3d vec3d = this.getAllowedMovement(pos);
            if (pos.x > vec3d.x) {
                block = block.getRelative(BlockFace.EAST);
            } else if (vec3d.x < vec3d.x) {
                block = block.getRelative(BlockFace.WEST);
            } else if (pos.z > vec3d.z) {
                block = block.getRelative(BlockFace.SOUTH);
            } else if (pos.z < vec3d.z) {
                block = block.getRelative(BlockFace.NORTH);
            }

            if (block.getType() != org.bukkit.Material.AIR) {
                VehicleBlockCollisionEvent event = new VehicleBlockCollisionEvent(vehicle, block);
                Bukkit.getPluginManager().callEvent(event);
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    @Nullable
    public Entity changeDimension(DimensionType destination, ITeleporter teleporter) {
        BlockPos location = ((InternalEntityBridge) this).internal$capturedPos();
        if (!ForgeHooks.onTravelToDimension((Entity) (Object) this, destination)) return null;
        if (!this.world.isRemote && !this.removed) {
            this.world.getProfiler().startSection("changeDimension");
            MinecraftServer minecraftserver = this.getServer();
            DimensionType dimensiontype = this.dimension;
            ServerWorld serverworld = minecraftserver.getWorld(dimensiontype);
            ServerWorld[] serverworld1 = new ServerWorld[]{minecraftserver.getWorld(destination)};

            if (serverworld1 == null) {
                return null;
            }
            //this.dimension = destination;
            //this.detach();
            this.world.getProfiler().startSection("reposition");
            Entity transportedEntity = teleporter.placeEntity((Entity) (Object) this, serverworld, serverworld1[0], this.rotationYaw, spawnPortal -> { //Forge: Start vanilla logic
                Vec3d vec3d = this.getMotion();
                float f = 0.0F;
                BlockPos blockpos = location;
                if (blockpos == null) {
                    if (dimensiontype == DimensionType.THE_END && destination == DimensionType.OVERWORLD) {
                        EntityPortalEvent event = CraftEventFactory.callEntityPortalEvent((Entity) (Object) this, serverworld1[0], serverworld1[0].getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, serverworld1[0].getSpawnPoint()), 0);
                        if (event == null) {
                            return null;
                        }
                        serverworld1[0] = ((CraftWorld) event.getTo().getWorld()).getHandle();
                        blockpos = new BlockPos(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());
                        //blockpos = serverworld1[0].getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, serverworld1[0].getSpawnPoint());
                    } else if (destination == DimensionType.THE_END) {
                        EntityPortalEvent event = CraftEventFactory.callEntityPortalEvent((Entity) (Object) this, serverworld1[0], (serverworld1[0].getSpawnCoordinate() != null) ? serverworld1[0].getSpawnCoordinate() : serverworld1[0].getSpawnPoint(), 0);
                        if (event == null) {
                            return null;
                        }
                        serverworld1[0] = ((CraftWorld) event.getTo().getWorld()).getHandle();
                        blockpos = new BlockPos(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());
                        //blockpos = serverworld1[0].getSpawnCoordinate();
                    } else {
                        double movementFactor = serverworld.getDimension().getMovementFactor() / serverworld1[0].getDimension().getMovementFactor();
                        double d0 = this.getPosX() * movementFactor;
                        double d1 = this.getPosZ() * movementFactor;

                        double d3 = Math.min(-2.9999872E7D, serverworld1[0].getWorldBorder().minX() + 16.0D);
                        double d4 = Math.min(-2.9999872E7D, serverworld1[0].getWorldBorder().minZ() + 16.0D);
                        double d5 = Math.min(2.9999872E7D, serverworld1[0].getWorldBorder().maxX() - 16.0D);
                        double d6 = Math.min(2.9999872E7D, serverworld1[0].getWorldBorder().maxZ() - 16.0D);
                        d0 = MathHelper.clamp(d0, d3, d5);
                        d1 = MathHelper.clamp(d1, d4, d6);
                        Vec3d vec3d1 = this.getLastPortalVec();
                        blockpos = new BlockPos(d0, this.getPosY(), d1);

                        EntityPortalEvent event2 = CraftEventFactory.callEntityPortalEvent((Entity) (Object) this, serverworld1[0], blockpos, 128);
                        if (event2 == null) {
                            return null;
                        }
                        serverworld1[0] = ((CraftWorld) event2.getTo().getWorld()).getHandle();
                        blockpos = new BlockPos(event2.getTo().getX(), event2.getTo().getY(), event2.getTo().getZ());
                        int searchRadius = event2.getSearchRadius();
                        // todo 实现 radius

                        if (spawnPortal) {
                            BlockPattern.PortalInfo blockpattern$portalinfo = serverworld1[0].getDefaultTeleporter().placeInExistingPortal(blockpos, vec3d, this.getTeleportDirection(), vec3d1.x, vec3d1.y, (Object) this instanceof PlayerEntity);
                            if (blockpattern$portalinfo == null) {
                                return null;
                            }

                            blockpos = new BlockPos(blockpattern$portalinfo.pos);
                            vec3d = blockpattern$portalinfo.motion;
                            f = (float) blockpattern$portalinfo.rotation;
                        }
                    }
                }

                this.dimension = destination;
                this.detach();

                this.world.getProfiler().endStartSection("reloading");
                Entity entity = this.getType().create(serverworld1[0]);
                if (entity != null) {
                    entity.copyDataFromOld((Entity) (Object) this);
                    entity.moveToBlockPosAndAngles(blockpos, entity.rotationYaw + f, entity.rotationPitch);
                    entity.setMotion(vec3d);
                    serverworld1[0].addFromAnotherDimension(entity);

                    ((InternalEntityBridge) this).internal$getBukkitEntity().setHandle(entity);
                    ((EntityBridge) entity).bridge$setBukkitEntity(((InternalEntityBridge) this).internal$getBukkitEntity());
                    if ((Object) this instanceof MobEntity) {
                        ((MobEntity) (Object) this).clearLeashed(true, false);
                    }
                }
                return entity;
            });//Forge: End vanilla logic

            this.remove(false);
            this.world.getProfiler().endSection();
            serverworld.resetUpdateEntityTick();
            serverworld1[0].resetUpdateEntityTick();
            this.world.getProfiler().endSection();
            return transportedEntity;
        } else {
            return null;
        }
    }

    @Override
    public double bridge$getEyeHeight() {
        return this.getPosYEye();
    }
}
