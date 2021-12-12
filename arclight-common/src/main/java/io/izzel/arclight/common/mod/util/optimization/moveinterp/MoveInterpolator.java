package io.izzel.arclight.common.mod.util.optimization.moveinterp;

import io.izzel.arclight.common.bridge.core.world.ServerEntityBridge;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.phys.Vec3;

import java.lang.invoke.VarHandle;
import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MoveInterpolator implements Runnable {

    private final ConcurrentHashMap<ServerEntity, EntityInterpolator> map = new ConcurrentHashMap<>();
    private final TickPredictor predictor = new TickPredictor.AvgTick();

    public void add(ServerEntity entity) {
        map.put(entity, new EntityInterpolator(entity));
    }

    public void remove(ServerEntity entity) {
        map.remove(entity);
    }

    public void startTracking(ServerEntity entity, ServerPlayer player) {
        map.get(entity).connections.add(player.connection);
    }

    public void stopTracking(ServerEntity entity, ServerPlayer player) {
        var interpolator = map.get(entity);
        if (interpolator != null) {
            interpolator.connections.remove(player.connection);
        }
    }

    private volatile double tickRate = 50d;
    private long lastTick = System.currentTimeMillis();

    public void mainThreadTick() {
        tickRate = predictor.tick();
        var elapsed = System.currentTimeMillis() - lastTick;
        lastTick = System.currentTimeMillis();
        var tickCount = elapsed / 50F;
        for (var entry : map.entrySet()) {
            entry.getValue().update(tickCount, entry.getKey());
        }
    }

    @Override
    public void run() {
        for (var interpolator : map.values()) {
            interpolator.tick();
        }
    }

    private record PathPoint(Vec3 pos, Vec3 motion, boolean onGround, boolean syncPosition,
                             byte yRot, byte xRot, float ticks, int flags) {

        public boolean isPassenger() {
            return (flags & 0x4) != 0;
        }
    }

    private class EntityInterpolator {

        private static final int DELAY_FACTOR = 0;

        private final Queue<PathPoint> queue = new ConcurrentLinkedQueue<>();
        private final Set<ServerPlayerConnection> connections = Collections.newSetFromMap(new ConcurrentHashMap<>());
        private final int entityId;
        private final int updateInterval;
        private final boolean trackMotion;
        private int countDown;
        private int updateTimer;
        private int teleportTimer;
        private PathPoint pathPoint;
        private float pathPointTicks;
        private Vec3 currentPos;
        private Vec3 currentMotion;
        private byte currentYRot;
        private byte currentXRot;
        private boolean onGround;

        public EntityInterpolator(ServerEntity entity) {
            this.entityId = ((ServerEntityBridge) entity).bridge$getTrackingEntity().getId();
            this.updateInterval = ((ServerEntityBridge) entity).bridge$getTrackingEntity().getType().updateInterval();
            this.trackMotion = ((ServerEntityBridge) entity).bridge$getTrackingEntity().getType().trackDeltas();
            this.currentPos = ((ServerEntityBridge) entity).bridge$getTrackingEntity().position();
            this.currentMotion = ((ServerEntityBridge) entity).bridge$getTrackingEntity().getDeltaMovement();
            this.countDown = DELAY_FACTOR;
            VarHandle.releaseFence();
        }

        public void update(float tickCount, ServerEntity entity) {
            var syncPosition = ((ServerEntityBridge) entity).bridge$syncPosition();
            var flags = 0;
            if (((ServerEntityBridge) entity).bridge$instantSyncPosition()) {
                flags |= 0x1;
            }
            if (((ServerEntityBridge) entity).bridge$instantSyncMotion()) {
                flags |= 0x2;
            }
            if (((ServerEntityBridge) entity).bridge$getTrackingEntity().isPassenger()) {
                flags |= 0x4;
            }
            var pathPoint = new PathPoint(((ServerEntityBridge) entity).bridge$getTrackingEntity().position(),
                ((ServerEntityBridge) entity).bridge$getTrackingEntity().getDeltaMovement(),
                ((ServerEntityBridge) entity).bridge$getTrackingEntity().isOnGround(),
                syncPosition,
                (byte) ((int) (((ServerEntityBridge) entity).bridge$getTrackingEntity().getYRot() * 256.0F / 360.0F)),
                (byte) ((int) (((ServerEntityBridge) entity).bridge$getTrackingEntity().getXRot() * 256.0F / 360.0F)),
                tickCount, flags);
            this.queue.add(pathPoint);
        }

        public void tick() {
            if (this.countDown > 0) {
                this.countDown--;
                return;
            }
            this.updateTimer++;
            var flags = this.pathPoint != null ? this.pathPoint.flags : 0;
            var instantSyncPosition = (flags & 0x1) != 0;
            var instantSyncMotion = (flags & 0x2) != 0;
            if ((this.updateTimer % this.updateInterval) == 0 || instantSyncPosition || instantSyncMotion) {
                var ticks = this.updateInterval;
                if (instantSyncPosition || instantSyncMotion) {
                    ticks = 1;
                }
                this.advance(this.keepUp(ticks), instantSyncMotion);
            }
        }

        private float keepUp(float ticks) {
            var delay = this.pathPoint == null ? 0f : this.pathPoint.ticks - this.pathPointTicks;
            for (var pathPoint : this.queue) {
                delay += pathPoint.ticks;
            }
            delay -= ticks;
            if (delay > this.updateInterval + DELAY_FACTOR) {
                ticks += delay - this.updateInterval - DELAY_FACTOR;
            }
            return ticks;
        }

        private void advance(float ticks, boolean instantSyncMotion) {
            var prevPointPos = this.currentPos;
            while (ticks > 1E-6) {
                if (this.pathPoint == null || Math.abs(this.pathPointTicks - this.pathPoint.ticks) < 1E-6) {
                    var pathPoint = this.queue.poll();
                    if (pathPoint == null) {
                        break;
                    } else {
                        if (this.pathPoint != null) {
                            prevPointPos = this.pathPoint.pos;
                        }
                        this.pathPoint = pathPoint;
                        if (pathPoint.syncPosition) {
                            this.pathPointTicks = pathPoint.ticks;
                            ticks -= pathPoint.ticks;
                            this.sendTeleportPacket(pathPoint);
                            continue;
                        } else {
                            this.pathPointTicks = 0;
                        }
                    }
                }
                var elapsed = Math.min(ticks, this.pathPoint.ticks - this.pathPointTicks);
                ticks -= elapsed;
                this.pathPointTicks += elapsed;
            }
            if (this.pathPoint == null) {
                throw new IllegalStateException("No path point");
            }
            var lerpOffset = this.pathPoint.pos.subtract(prevPointPos).scale(this.pathPointTicks / this.pathPoint.ticks);
            var offset = prevPointPos.add(lerpOffset).subtract(this.currentPos);
            long x = ClientboundMoveEntityPacket.entityToPacket(offset.x);
            long y = ClientboundMoveEntityPacket.entityToPacket(offset.y);
            long z = ClientboundMoveEntityPacket.entityToPacket(offset.z);
            boolean requireTeleport = x < -32768L || x > 32767L || y < -32768L || y > 32767L || z < -32768L || z > 32767L;
            boolean updatePosition = offset.lengthSqr() > 1E-6;
            boolean updateRotation = Math.abs(this.currentYRot - this.pathPoint.yRot) >= 1 || Math.abs(this.currentXRot - this.pathPoint.xRot) >= 1;
            if (this.pathPoint.isPassenger()) {
                if (updateRotation) {
                    this.sendPacket(new ClientboundMoveEntityPacket.Rot(this.entityId, this.pathPoint.yRot, this.pathPoint.xRot, this.pathPoint.onGround));
                    this.currentYRot = this.pathPoint.yRot;
                    this.currentXRot = this.pathPoint.xRot;
                }
            } else {
                if (!requireTeleport && this.teleportTimer++ <= 400 && this.onGround == this.pathPoint.onGround) {
                    if (updatePosition) {
                        if (updateRotation) {
                            this.sendPacket(new ClientboundMoveEntityPacket.PosRot(this.entityId, (short) x, (short) y, (short) z,
                                this.pathPoint.yRot, this.pathPoint.xRot, this.pathPoint.onGround));
                            this.currentYRot = this.pathPoint.yRot;
                            this.currentXRot = this.pathPoint.xRot;
                        } else {
                            this.sendPacket(new ClientboundMoveEntityPacket.Pos(this.entityId, (short) x, (short) y, (short) z, this.pathPoint.onGround));
                        }
                        this.currentPos = this.currentPos.add(offset);
                        this.onGround = this.pathPoint.onGround;
                    } else if (updateRotation) {
                        this.sendPacket(new ClientboundMoveEntityPacket.Rot(this.entityId, this.pathPoint.yRot, this.pathPoint.xRot, this.pathPoint.onGround));
                        this.currentYRot = this.pathPoint.yRot;
                        this.currentXRot = this.pathPoint.xRot;
                    }
                } else {
                    this.sendTeleportPacket(this.pathPoint);
                }
                if (this.trackMotion || instantSyncMotion) {
                    var newMotion = this.pathPoint.motion.scale(50D / Math.max(tickRate, 50D));
                    var distance = newMotion.distanceToSqr(this.currentMotion);
                    var requireMotionUpdate = distance > 1E-7D || distance > 0 && this.pathPoint.motion.lengthSqr() == 0.0D;
                    if (requireMotionUpdate) {
                        this.sendPacket(new ClientboundSetEntityMotionPacket(this.entityId, newMotion));
                        this.currentMotion = newMotion;
                    }
                }
            }
        }

        private void sendTeleportPacket(PathPoint pathPoint) {
            var buf = new FriendlyByteBuf(Unpooled.buffer(32));
            buf.writeVarInt(this.entityId)
                .writeDouble(pathPoint.pos.x)
                .writeDouble(pathPoint.pos.y)
                .writeDouble(pathPoint.pos.z)
                .writeByte(pathPoint.yRot)
                .writeByte(pathPoint.xRot)
                .writeBoolean(pathPoint.onGround);
            this.sendPacket(new ClientboundTeleportEntityPacket(buf));
            this.teleportTimer = 0;
            this.currentPos = pathPoint.pos;
            this.currentYRot = pathPoint.yRot;
            this.currentXRot = pathPoint.xRot;
            this.onGround = pathPoint.onGround;
        }

        private void sendPacket(Packet<?> packet) {
            for (var connection : this.connections) {
                connection.send(packet);
            }
        }
    }
}
