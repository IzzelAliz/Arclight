package io.izzel.arclight.common.mixin.core.world;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.world.TrackedEntityBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SEntityEquipmentPacket;
import net.minecraft.network.play.server.SEntityHeadLookPacket;
import net.minecraft.network.play.server.SEntityMetadataPacket;
import net.minecraft.network.play.server.SEntityPacket;
import net.minecraft.network.play.server.SEntityPropertiesPacket;
import net.minecraft.network.play.server.SEntityTeleportPacket;
import net.minecraft.network.play.server.SEntityVelocityPacket;
import net.minecraft.network.play.server.SMountEntityPacket;
import net.minecraft.network.play.server.SPlayEntityEffectPacket;
import net.minecraft.network.play.server.SSetPassengersPacket;
import net.minecraft.network.play.server.SSpawnMobPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.TrackedEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.MapData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Mixin(TrackedEntity.class)
public abstract class TrackedEntityMixin implements TrackedEntityBridge {

    // @formatter:off
    @Shadow @Final private Entity trackedEntity;
    @Shadow private List<Entity> passengers;
    @Shadow @Final private Consumer<IPacket<?>> packetConsumer;
    @Shadow private int updateCounter;
    @Shadow @Final private ServerWorld world;
    @Shadow protected abstract void sendMetadata();
    @Shadow @Final private int updateFrequency;
    @Shadow private int encodedRotationYaw;
    @Shadow private int encodedRotationPitch;
    @Shadow protected abstract void updateEncodedPosition();
    @Shadow private boolean riding;
    @Shadow private int ticksSinceAbsoluteTeleport;
    @Shadow private long encodedPosX;
    @Shadow private long encodedPosY;
    @Shadow private long encodedPosZ;
    @Shadow private boolean onGround;
    @Shadow @Final private boolean sendVelocityUpdates;
    @Shadow private Vector3d velocity;
    @Shadow private int encodedRotationYawHead;
    @Shadow protected abstract void sendPacket(IPacket<?> packet);
    // @formatter:on

    private Set<ServerPlayerEntity> trackedPlayers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(ServerWorld serverWorld, Entity entity, int updateFrequency, boolean sendVelocityUpdates, Consumer<IPacket<?>> packetConsumer, CallbackInfo ci) {
        trackedPlayers = new HashSet<>();
    }

    public void arclight$constructor(ServerWorld serverWorld, Entity entity, int updateFrequency, boolean sendVelocityUpdates, Consumer<IPacket<?>> packetConsumer) {
        throw new NullPointerException();
    }

    public void arclight$constructor(ServerWorld serverWorld, Entity entity, int updateFrequency, boolean sendVelocityUpdates, Consumer<IPacket<?>> packetConsumer, Set<ServerPlayerEntity> set) {
        arclight$constructor(serverWorld, entity, updateFrequency, sendVelocityUpdates, packetConsumer);
        this.trackedPlayers = set;
    }

    @Override
    public void bridge$setTrackedPlayers(Set<ServerPlayerEntity> trackedPlayers) {
        this.trackedPlayers = trackedPlayers;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void tick() {
        List<Entity> list = this.trackedEntity.getPassengers();
        if (!list.equals(this.passengers)) {
            this.passengers = list;
            this.sendPacket(new SSetPassengersPacket(this.trackedEntity));
        }
        if (this.trackedEntity instanceof ItemFrameEntity) {
            ItemFrameEntity itemframeentity = (ItemFrameEntity) this.trackedEntity;
            ItemStack itemstack = itemframeentity.getDisplayedItem();
            MapData mapdata = FilledMapItem.getMapData(itemstack, this.world);
            if (this.updateCounter % 10 == 0 && mapdata != null) {
                for (ServerPlayerEntity serverplayerentity : this.trackedPlayers) {
                    mapdata.updateVisiblePlayers(serverplayerentity, itemstack);
                    IPacket<?> ipacket = ((FilledMapItem) itemstack.getItem()).getUpdatePacket(itemstack, this.world, serverplayerentity);
                    if (ipacket != null) {
                        serverplayerentity.connection.sendPacket(ipacket);
                    }
                }
            }
            this.sendMetadata();
        }
        if (this.updateCounter % this.updateFrequency == 0 || this.trackedEntity.isAirBorne || this.trackedEntity.getDataManager().isDirty()) {
            if (this.trackedEntity.isPassenger()) {
                int i1 = MathHelper.floor(this.trackedEntity.rotationYaw * 256.0F / 360.0F);
                int l1 = MathHelper.floor(this.trackedEntity.rotationPitch * 256.0F / 360.0F);
                boolean flag2 = Math.abs(i1 - this.encodedRotationYaw) >= 1 || Math.abs(l1 - this.encodedRotationPitch) >= 1;
                if (flag2) {
                    this.packetConsumer.accept(new SEntityPacket.LookPacket(this.trackedEntity.getEntityId(), (byte) i1, (byte) l1, this.trackedEntity.isOnGround()));
                    this.encodedRotationYaw = i1;
                    this.encodedRotationPitch = l1;
                }
                this.updateEncodedPosition();
                this.sendMetadata();
                this.riding = true;
            } else {
                ++this.ticksSinceAbsoluteTeleport;
                int l = MathHelper.floor(this.trackedEntity.rotationYaw * 256.0F / 360.0F);
                int k1 = MathHelper.floor(this.trackedEntity.rotationPitch * 256.0F / 360.0F);
                Vector3d vector3d = this.trackedEntity.getPositionVec().subtract(SEntityPacket.func_218744_a(this.encodedPosX, this.encodedPosY, this.encodedPosZ));
                boolean flag3 = vector3d.lengthSquared() >= (double) 7.6293945E-6F;
                IPacket<?> ipacket1 = null;
                boolean flag4 = flag3 || this.updateCounter % 60 == 0;
                boolean flag = Math.abs(l - this.encodedRotationYaw) >= 1 || Math.abs(k1 - this.encodedRotationPitch) >= 1;
                if (flag4) {
                    this.updateEncodedPosition();
                }
                if (flag) {
                    this.encodedRotationYaw = l;
                    this.encodedRotationPitch = k1;
                }
                if (this.updateCounter > 0 || this.trackedEntity instanceof AbstractArrowEntity) {
                    long i = SEntityPacket.func_218743_a(vector3d.x);
                    long j = SEntityPacket.func_218743_a(vector3d.y);
                    long k = SEntityPacket.func_218743_a(vector3d.z);
                    boolean flag1 = i < -32768L || i > 32767L || j < -32768L || j > 32767L || k < -32768L || k > 32767L;
                    if (!flag1 && this.ticksSinceAbsoluteTeleport <= 400 && !this.riding && this.onGround == this.trackedEntity.isOnGround()) {
                        if ((!flag4 || !flag) && !(this.trackedEntity instanceof AbstractArrowEntity)) {
                            if (flag4) {
                                ipacket1 = new SEntityPacket.RelativeMovePacket(this.trackedEntity.getEntityId(), (short) ((int) i), (short) ((int) j), (short) ((int) k), this.trackedEntity.isOnGround());
                            } else if (flag) {
                                ipacket1 = new SEntityPacket.LookPacket(this.trackedEntity.getEntityId(), (byte) l, (byte) k1, this.trackedEntity.isOnGround());
                            }
                        } else {
                            ipacket1 = new SEntityPacket.MovePacket(this.trackedEntity.getEntityId(), (short) ((int) i), (short) ((int) j), (short) ((int) k), (byte) l, (byte) k1, this.trackedEntity.isOnGround());
                        }
                    } else {
                        this.onGround = this.trackedEntity.isOnGround();
                        this.ticksSinceAbsoluteTeleport = 0;
                        ipacket1 = new SEntityTeleportPacket(this.trackedEntity);
                    }
                }
                if ((this.sendVelocityUpdates || this.trackedEntity.isAirBorne || this.trackedEntity instanceof LivingEntity && ((LivingEntity) this.trackedEntity).isElytraFlying()) && this.updateCounter > 0) {
                    Vector3d vector3d1 = this.trackedEntity.getMotion();
                    double d0 = vector3d1.squareDistanceTo(this.velocity);
                    if (d0 > 1.0E-7D || d0 > 0.0D && vector3d1.lengthSquared() == 0.0D) {
                        this.velocity = vector3d1;
                        this.packetConsumer.accept(new SEntityVelocityPacket(this.trackedEntity.getEntityId(), this.velocity));
                    }
                }
                if (ipacket1 != null) {
                    this.packetConsumer.accept(ipacket1);
                }
                this.sendMetadata();
                this.riding = false;
            }
            int j1 = MathHelper.floor(this.trackedEntity.getRotationYawHead() * 256.0F / 360.0F);
            if (Math.abs(j1 - this.encodedRotationYawHead) >= 1) {
                this.packetConsumer.accept(new SEntityHeadLookPacket(this.trackedEntity, (byte) j1));
                this.encodedRotationYawHead = j1;
            }
            this.trackedEntity.isAirBorne = false;
        }
        ++this.updateCounter;
        if (this.trackedEntity.velocityChanged) {
            boolean cancelled = false;
            if (this.trackedEntity instanceof ServerPlayerEntity) {
                Player player = ((ServerPlayerEntityBridge) this.trackedEntity).bridge$getBukkitEntity();
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
                this.sendPacket(new SEntityVelocityPacket(this.trackedEntity));
            }
            this.trackedEntity.velocityChanged = false;
        }
    }

    @Inject(method = "track", at = @At("HEAD"))
    private void arclight$addPlayer(ServerPlayerEntity player, CallbackInfo ci) {
        this.arclight$player = player;
    }

    private transient ServerPlayerEntity arclight$player;

    public void a(final Consumer<IPacket<?>> consumer, ServerPlayerEntity playerEntity) {
        this.arclight$player = playerEntity;
        this.sendSpawnPackets(consumer);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void sendSpawnPackets(final Consumer<IPacket<?>> consumer) {
        ServerPlayerEntity player = arclight$player;
        arclight$player = null;
        MobEntity entityinsentient;
        if (this.trackedEntity.removed) {
            return;
        }
        IPacket<?> packet = this.trackedEntity.createSpawnPacket();
        this.encodedRotationYawHead = MathHelper.floor(this.trackedEntity.getRotationYawHead() * 256.0f / 360.0f);
        consumer.accept(packet);
        if (!this.trackedEntity.getDataManager().isEmpty()) {
            consumer.accept(new SEntityMetadataPacket(this.trackedEntity.getEntityId(), this.trackedEntity.getDataManager(), true));
        }
        boolean flag = this.sendVelocityUpdates;
        if (this.trackedEntity instanceof LivingEntity) {
            Collection<ModifiableAttributeInstance> collection = ((LivingEntity) this.trackedEntity).getAttributeManager().getWatchedInstances();
            if (this.trackedEntity.getEntityId() == player.getEntityId()) {
                ((ServerPlayerEntityBridge) this.trackedEntity).bridge$getBukkitEntity().injectScaledMaxHealth(collection, false);
            }
            if (!collection.isEmpty()) {
                consumer.accept(new SEntityPropertiesPacket(this.trackedEntity.getEntityId(), collection));
            }
            if (((LivingEntity) this.trackedEntity).isElytraFlying()) {
                flag = true;
            }
        }
        this.velocity = this.trackedEntity.getMotion();
        if (flag && !(packet instanceof SSpawnMobPacket)) {
            consumer.accept(new SEntityVelocityPacket(this.trackedEntity.getEntityId(), this.velocity));
        }
        if (this.trackedEntity instanceof LivingEntity) {
            ArrayList<Pair<EquipmentSlotType, ItemStack>> list = Lists.newArrayList();
            for (EquipmentSlotType enumitemslot : EquipmentSlotType.values()) {
                ItemStack itemstack = ((LivingEntity) this.trackedEntity).getItemStackFromSlot(enumitemslot);
                if (itemstack.isEmpty()) continue;
                list.add(Pair.of(enumitemslot, itemstack.copy()));
            }
            if (!list.isEmpty()) {
                consumer.accept(new SEntityEquipmentPacket(this.trackedEntity.getEntityId(), list));
            }
        }
        this.encodedRotationYawHead = MathHelper.floor(this.trackedEntity.getRotationYawHead() * 256.0f / 360.0f);
        consumer.accept(new SEntityHeadLookPacket(this.trackedEntity, (byte) this.encodedRotationYawHead));
        if (this.trackedEntity instanceof LivingEntity) {
            LivingEntity entityliving = (LivingEntity) this.trackedEntity;
            for (EffectInstance mobeffect : entityliving.getActivePotionEffects()) {
                consumer.accept(new SPlayEntityEffectPacket(this.trackedEntity.getEntityId(), mobeffect));
            }
        }
        if (!this.trackedEntity.getPassengers().isEmpty()) {
            consumer.accept(new SSetPassengersPacket(this.trackedEntity));
        }
        if (this.trackedEntity.isPassenger()) {
            consumer.accept(new SSetPassengersPacket(this.trackedEntity.getRidingEntity()));
        }
        if (this.trackedEntity instanceof MobEntity && (entityinsentient = (MobEntity) this.trackedEntity).getLeashed()) {
            consumer.accept(new SMountEntityPacket(entityinsentient, entityinsentient.getLeashHolder()));
        }
    }

    @Inject(method = "sendMetadata", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/TrackedEntity;sendPacket(Lnet/minecraft/network/IPacket;)V"))
    private void arclight$sendScaledHealth(CallbackInfo ci, EntityDataManager entitydatamanager, Set<ModifiableAttributeInstance> set) {
        if (this.trackedEntity instanceof ServerPlayerEntity) {
            ((ServerPlayerEntityBridge) this.trackedEntity).bridge$getBukkitEntity().injectScaledMaxHealth(set, false);
        }
    }
}
