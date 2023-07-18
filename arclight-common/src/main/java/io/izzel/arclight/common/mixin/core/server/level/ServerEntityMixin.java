package io.izzel.arclight.common.mixin.core.server.level;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.world.ServerEntityBridge;
import io.izzel.arclight.common.mod.ArclightConstants;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin implements ServerEntityBridge {

    // @formatter:off
    @Shadow @Final private Entity entity;
    @Shadow private List<Entity> lastPassengers;
    @Shadow @Final private Consumer<Packet<?>> broadcast;
    @Shadow private int tickCount;
    @Shadow @Final private ServerLevel level;
    @Shadow protected abstract void sendDirtyEntityData();
    @Shadow @Final private int updateInterval;
    @Shadow private int yRotp;
    @Shadow private int xRotp;
    @Shadow @Final private VecDeltaCodec positionCodec;
    @Shadow private boolean wasRiding;
    @Shadow private int teleportDelay;
    @Shadow private boolean wasOnGround;
    @Shadow @Final private boolean trackDelta;
    @Shadow private Vec3 ap;
    @Shadow private int yHeadRotp;
    @Shadow protected abstract void broadcastAndSend(Packet<?> packet);
    @Shadow @Nullable private List<SynchedEntityData.DataValue<?>> trackedDataValues;
    @Shadow protected abstract Stream<Entity> changedPassengers(List<Entity> p_275537_, List<Entity> p_275682_);
    // @formatter:on

    private Set<ServerPlayerConnection> trackedPlayers;
    @Unique private int lastTick;
    @Unique private int lastUpdate, lastPosUpdate, lastMapUpdate;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(ServerLevel serverWorld, Entity entity, int updateFrequency, boolean sendVelocityUpdates, Consumer<Packet<?>> packetConsumer, CallbackInfo ci) {
        trackedPlayers = new HashSet<>();
        lastTick = ArclightConstants.currentTick - 1;
        lastUpdate = lastPosUpdate = lastMapUpdate = -1;
    }

    public void arclight$constructor(ServerLevel serverWorld, Entity entity, int updateFrequency, boolean sendVelocityUpdates, Consumer<Packet<?>> packetConsumer) {
        throw new NullPointerException();
    }

    public void arclight$constructor(ServerLevel serverWorld, Entity entity, int updateFrequency, boolean sendVelocityUpdates, Consumer<Packet<?>> packetConsumer, Set<ServerPlayerConnection> set) {
        arclight$constructor(serverWorld, entity, updateFrequency, sendVelocityUpdates, packetConsumer);
        this.trackedPlayers = set;
    }

    @Override
    public void bridge$setTrackedPlayers(Set<ServerPlayerConnection> trackedPlayers) {
        this.trackedPlayers = trackedPlayers;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void sendChanges() {
        List<Entity> list = this.entity.getPassengers();
        if (!list.equals(this.lastPassengers)) {
            this.lastPassengers = list;
            this.broadcastAndSend(new ClientboundSetPassengersPacket(this.entity));
            this.changedPassengers(list, this.lastPassengers).forEach((p_275907_) -> {
                if (p_275907_ instanceof ServerPlayer serverplayer1) {
                    if (!list.contains(serverplayer1)) {
                        serverplayer1.connection.teleport(serverplayer1.getX(), serverplayer1.getY(), serverplayer1.getZ(), serverplayer1.getYRot(), serverplayer1.getXRot());
                    }
                }
            });
        }
        int elapsedTicks = ArclightConstants.currentTick - this.lastTick;
        if (elapsedTicks < 0) {
            elapsedTicks = 0;
        }
        this.lastTick = ArclightConstants.currentTick;
        if (this.entity instanceof ItemFrame itemFrame) {
            ItemStack itemstack = itemFrame.getItem();
            if (this.tickCount / 10 != this.lastMapUpdate && itemstack.getItem() instanceof MapItem) {
                MapItemSavedData mapdata = MapItem.getSavedData(itemstack, this.level);
                if (mapdata != null) {
                    for (ServerPlayerConnection connection : this.trackedPlayers) {
                        var serverplayerentity = connection.getPlayer();
                        mapdata.tickCarriedBy(serverplayerentity, itemstack);
                        Packet<?> ipacket = ((MapItem) itemstack.getItem()).getUpdatePacket(itemstack, this.level, serverplayerentity);
                        if (ipacket != null) {
                            serverplayerentity.connection.send(ipacket);
                        }
                    }
                }
            }
            this.sendDirtyEntityData();
        }
        if (this.tickCount / this.updateInterval != this.lastUpdate || this.entity.hasImpulse || this.entity.getEntityData().isDirty()) {
            if (this.entity.isPassenger()) {
                int i1 = Mth.floor(this.entity.getYRot() * 256.0F / 360.0F);
                int l1 = Mth.floor(this.entity.getXRot() * 256.0F / 360.0F);
                boolean flag2 = Math.abs(i1 - this.yRotp) >= 1 || Math.abs(l1 - this.xRotp) >= 1;
                if (flag2) {
                    this.broadcast.accept(new ClientboundMoveEntityPacket.Rot(this.entity.getId(), (byte) i1, (byte) l1, this.entity.isOnGround()));
                    this.yRotp = i1;
                    this.xRotp = l1;
                }
                this.positionCodec.setBase(this.entity.trackingPosition());
                this.sendDirtyEntityData();
                this.wasRiding = true;
            } else {
                this.teleportDelay += elapsedTicks;
                int l = Mth.floor(this.entity.getYRot() * 256.0F / 360.0F);
                int k1 = Mth.floor(this.entity.getXRot() * 256.0F / 360.0F);
                Vec3 vector3d = this.entity.trackingPosition();
                boolean flag3 = this.positionCodec.delta(vector3d).lengthSqr() >= 7.62939453125E-6D;
                Packet<?> ipacket1 = null;
                boolean flag4 = flag3 || this.tickCount / 60 != this.lastPosUpdate;
                boolean flag = Math.abs(l - this.yRotp) >= 1 || Math.abs(k1 - this.xRotp) >= 1;
                boolean pos = false;
                boolean rot = false;
                if (this.tickCount > 0 || this.entity instanceof AbstractArrow) {
                    long i = this.positionCodec.encodeX(vector3d);
                    long j = this.positionCodec.encodeY(vector3d);
                    long k = this.positionCodec.encodeZ(vector3d);
                    boolean flag1 = i < -32768L || i > 32767L || j < -32768L || j > 32767L || k < -32768L || k > 32767L;
                    if (!flag1 && this.teleportDelay <= 400 && !this.wasRiding && this.wasOnGround == this.entity.isOnGround()) {
                        if ((!flag4 || !flag) && !(this.entity instanceof AbstractArrow)) {
                            if (flag4) {
                                ipacket1 = new ClientboundMoveEntityPacket.Pos(this.entity.getId(), (short) ((int) i), (short) ((int) j), (short) ((int) k), this.entity.isOnGround());
                                pos = true;
                            } else if (flag) {
                                ipacket1 = new ClientboundMoveEntityPacket.Rot(this.entity.getId(), (byte) l, (byte) k1, this.entity.isOnGround());
                                rot = true;
                            }
                        } else {
                            ipacket1 = new ClientboundMoveEntityPacket.PosRot(this.entity.getId(), (short) ((int) i), (short) ((int) j), (short) ((int) k), (byte) l, (byte) k1, this.entity.isOnGround());
                            pos = rot = true;
                        }
                    } else {
                        this.wasOnGround = this.entity.isOnGround();
                        this.teleportDelay = 0;
                        ipacket1 = new ClientboundTeleportEntityPacket(this.entity);
                        pos = rot = true;
                    }
                }
                if ((this.trackDelta || this.entity.hasImpulse || this.entity instanceof LivingEntity && ((LivingEntity) this.entity).isFallFlying()) && this.tickCount > 0) {
                    Vec3 vector3d1 = this.entity.getDeltaMovement();
                    double d0 = vector3d1.distanceToSqr(this.ap);
                    if (d0 > 1.0E-7D || d0 > 0.0D && vector3d1.lengthSqr() == 0.0D) {
                        this.ap = vector3d1;
                        this.broadcast.accept(new ClientboundSetEntityMotionPacket(this.entity.getId(), this.ap));
                    }
                }
                if (ipacket1 != null) {
                    this.broadcast.accept(ipacket1);
                }
                this.sendDirtyEntityData();
                if (pos) {
                    this.positionCodec.setBase(vector3d);
                }
                if (rot) {
                    this.yRotp = l;
                    this.xRotp = k1;
                }
                this.wasRiding = false;
            }
            int j1 = Mth.floor(this.entity.getYHeadRot() * 256.0F / 360.0F);
            if (Math.abs(j1 - this.yHeadRotp) >= 1) {
                this.broadcast.accept(new ClientboundRotateHeadPacket(this.entity, (byte) j1));
                this.yHeadRotp = j1;
            }
            this.entity.hasImpulse = false;
        }
        this.lastUpdate = this.tickCount / this.updateInterval;
        this.lastPosUpdate = this.tickCount / 60;
        this.lastMapUpdate = this.tickCount / 10;
        this.tickCount += elapsedTicks;
        if (this.entity.hurtMarked) {
            boolean cancelled = false;
            if (this.entity instanceof ServerPlayer) {
                Player player = ((ServerPlayerEntityBridge) this.entity).bridge$getBukkitEntity();
                Vector velocity = player.getVelocity();
                PlayerVelocityEvent event = new PlayerVelocityEvent(player, velocity.clone());
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    cancelled = true;
                } else if (!velocity.equals(event.getVelocity())) {
                    player.setVelocity(event.getVelocity());
                }
            }
            if (!cancelled) {
                this.broadcastAndSend(new ClientboundSetEntityMotionPacket(this.entity));
            }
            this.entity.hurtMarked = false;
        }
    }

    @Inject(method = "addPairing", at = @At("HEAD"))
    private void arclight$addPlayer(ServerPlayer player, CallbackInfo ci) {
        this.arclight$player = player;
    }

    private transient ServerPlayer arclight$player;

    public void sendPairingData(final Consumer<Packet<?>> consumer, ServerPlayer playerEntity) { // CraftBukkit - add player
        this.arclight$player = playerEntity;
        this.sendPairingData(consumer);
    }

    public void a(final Consumer<Packet<?>> consumer, ServerPlayer playerEntity) { // for backward compatability
        this.sendPairingData(consumer, playerEntity);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void sendPairingData(final Consumer<Packet<?>> consumer) {
        ServerPlayer player = arclight$player;
        arclight$player = null;
        Mob entityinsentient;
        if (this.entity.isRemoved()) {
            return;
        }
        Packet<?> packet = this.entity.getAddEntityPacket();
        this.yHeadRotp = Mth.floor(this.entity.getYHeadRot() * 256.0f / 360.0f);
        consumer.accept(packet);
        if (this.trackedDataValues != null) {
            consumer.accept(new ClientboundSetEntityDataPacket(this.entity.getId(), this.trackedDataValues));
        }
        boolean flag = this.trackDelta;
        if (this.entity instanceof LivingEntity livingEntity) {
            Collection<AttributeInstance> collection = livingEntity.getAttributes().getSyncableAttributes();
            if (this.entity.getId() == player.getId()) {
                ((ServerPlayerEntityBridge) this.entity).bridge$getBukkitEntity().injectScaledMaxHealth(collection, false);
            }
            if (!collection.isEmpty()) {
                consumer.accept(new ClientboundUpdateAttributesPacket(this.entity.getId(), collection));
            }
            if (livingEntity.isFallFlying()) {
                flag = true;
            }
        }
        this.ap = this.entity.getDeltaMovement();
        if (flag && !(this.entity instanceof LivingEntity)) {
            consumer.accept(new ClientboundSetEntityMotionPacket(this.entity.getId(), this.ap));
        }
        if (this.entity instanceof LivingEntity) {
            ArrayList<Pair<EquipmentSlot, ItemStack>> list = Lists.newArrayList();
            for (EquipmentSlot enumitemslot : EquipmentSlot.values()) {
                ItemStack itemstack = ((LivingEntity) this.entity).getItemBySlot(enumitemslot);
                if (itemstack.isEmpty()) continue;
                list.add(Pair.of(enumitemslot, itemstack.copy()));
            }
            if (!list.isEmpty()) {
                consumer.accept(new ClientboundSetEquipmentPacket(this.entity.getId(), list));
            }
        }
        this.yHeadRotp = Mth.floor(this.entity.getYHeadRot() * 256.0f / 360.0f);
        consumer.accept(new ClientboundRotateHeadPacket(this.entity, (byte) this.yHeadRotp));
        if (this.entity instanceof LivingEntity livingEntity) {
            for (MobEffectInstance mobeffect : livingEntity.getActiveEffects()) {
                consumer.accept(new ClientboundUpdateMobEffectPacket(this.entity.getId(), mobeffect));
            }
            livingEntity.detectEquipmentUpdates();
        }
        if (!this.entity.getPassengers().isEmpty()) {
            consumer.accept(new ClientboundSetPassengersPacket(this.entity));
        }
        if (this.entity.isPassenger()) {
            consumer.accept(new ClientboundSetPassengersPacket(this.entity.getVehicle()));
        }
        if (this.entity instanceof Mob && (entityinsentient = (Mob) this.entity).isLeashed()) {
            consumer.accept(new ClientboundSetEntityLinkPacket(entityinsentient, entityinsentient.getLeashHolder()));
        }
    }

    @Inject(method = "sendDirtyEntityData", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/server/level/ServerEntity;broadcastAndSend(Lnet/minecraft/network/protocol/Packet;)V"))
    private void arclight$sendScaledHealth(CallbackInfo ci, SynchedEntityData entitydatamanager, List<SynchedEntityData.DataValue<?>> list, Set<AttributeInstance> set) {
        if (this.entity instanceof ServerPlayerEntityBridge player) {
            player.bridge$getBukkitEntity().injectScaledMaxHealth(set, false);
        }
    }
}
