package io.izzel.arclight.impl.mixin.v1_14.entity;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
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
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.event.entity.EntityPortalEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(Entity.class)
public abstract class EntityMixin_1_14 {

    // @formatter:off
    @Shadow public World world;
    @Shadow @Deprecated public boolean removed;
    @Shadow public DimensionType dimension;
    @Shadow public double posX;
    @Shadow public double posZ;
    @Shadow public double posY;
    @Shadow @Nullable public abstract MinecraftServer getServer();
    @Shadow public abstract Vec3d getMotion();
    @Shadow public abstract Vec3d getLastPortalVec();
    @Shadow public abstract boolean isAlive();
    @Shadow public abstract Direction getTeleportDirection();
    @Shadow public abstract void detach();
    @Shadow public abstract EntityType<?> getType();
    @Shadow public abstract void remove(boolean keepData);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    @Nullable
    public Entity changeDimension(DimensionType destination) {
        BlockPos location = ((InternalEntityBridge) this).internal$capturedPos();
        if (!ForgeHooks.onTravelToDimension((Entity) (Object) this, destination)) return null;
        if (!this.world.isRemote && !this.removed) {
            this.world.getProfiler().startSection("changeDimension");
            MinecraftServer minecraftserver = this.getServer();
            DimensionType dimensiontype = this.dimension;
            ServerWorld serverworld = minecraftserver.func_71218_a(dimensiontype);
            ServerWorld serverworld1 = minecraftserver.func_71218_a(destination);
            if (serverworld1 == null) {
                return null;
            }
            // this.dimension = destination;
            // this.detach();
            this.world.getProfiler().startSection("reposition");
            Vec3d vec3d = this.getMotion();
            float f = 0.0F;
            BlockPos blockpos = location;
            if (blockpos == null) {
                if (dimensiontype == DimensionType.THE_END && destination == DimensionType.OVERWORLD) {
                    blockpos = serverworld1.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, serverworld1.getSpawnPoint());
                } else if (destination == DimensionType.THE_END) {
                    blockpos = serverworld1.getSpawnCoordinate();
                } else {
                    double movementFactor = serverworld.getDimension().getMovementFactor() / serverworld1.getDimension().getMovementFactor();
                    double d0 = this.posX * movementFactor;
                    double d1 = this.posZ * movementFactor;

                    double d3 = Math.min(-2.9999872E7D, serverworld1.getWorldBorder().minX() + 16.0D);
                    double d4 = Math.min(-2.9999872E7D, serverworld1.getWorldBorder().minZ() + 16.0D);
                    double d5 = Math.min(2.9999872E7D, serverworld1.getWorldBorder().maxX() - 16.0D);
                    double d6 = Math.min(2.9999872E7D, serverworld1.getWorldBorder().maxZ() - 16.0D);
                    d0 = MathHelper.clamp(d0, d3, d5);
                    d1 = MathHelper.clamp(d1, d4, d6);
                    Vec3d vec3d1 = this.getLastPortalVec();
                    blockpos = new BlockPos(d0, this.posY, d1);
                    BlockPattern.PortalInfo blockpattern$portalinfo = serverworld1.getDefaultTeleporter().placeInExistingPortal(blockpos, vec3d, this.getTeleportDirection(), vec3d1.x, vec3d1.y, (Object) this instanceof PlayerEntity);
                    if (blockpattern$portalinfo == null) {
                        return null;
                    }

                    blockpos = new BlockPos(blockpattern$portalinfo.pos);
                    vec3d = blockpattern$portalinfo.motion;
                    f = (float) blockpattern$portalinfo.rotation;
                }
            }

            if (location == null) {
                Location enter = ((InternalEntityBridge) this).internal$getBukkitEntity().getLocation();
                Location exit = new Location(((ServerWorldBridge) serverworld1).bridge$getWorld(), blockpos.getX(), blockpos.getY(), blockpos.getZ());

                EntityPortalEvent event = new EntityPortalEvent(((InternalEntityBridge) this).internal$getBukkitEntity(), enter, exit);
                event.getEntity().getServer().getPluginManager().callEvent(event);
                if (event.isCancelled() || event.getTo() == null || event.getTo().getWorld() == null || !this.isAlive()) {
                    return null;
                }

                exit = event.getTo();
                serverworld1 = ((CraftWorld) exit.getWorld()).getHandle();
                blockpos = new BlockPos(exit.getX(), exit.getY(), exit.getZ());
            }

            this.dimension = destination;
            this.detach();

            this.world.getProfiler().endStartSection("reloading");
            Entity entity = this.getType().create(serverworld1);
            if (entity != null) {
                entity.copyDataFromOld((Entity) (Object) this);
                entity.moveToBlockPosAndAngles(blockpos, entity.rotationYaw + f, entity.rotationPitch);
                entity.setMotion(vec3d);
                serverworld1.func_217460_e(entity);

                ((InternalEntityBridge) this).internal$getBukkitEntity().setHandle(entity);
                ((EntityBridge) entity).bridge$setBukkitEntity(((InternalEntityBridge) this).internal$getBukkitEntity());
                if ((Object) this instanceof MobEntity) {
                    ((MobEntity) (Object) this).clearLeashed(true, false);
                }
            }

            this.remove(false);
            this.world.getProfiler().endSection();
            serverworld.resetUpdateEntityTick();
            serverworld1.resetUpdateEntityTick();
            this.world.getProfiler().endSection();
            return entity;
        } else {
            return null;
        }
    }
}
