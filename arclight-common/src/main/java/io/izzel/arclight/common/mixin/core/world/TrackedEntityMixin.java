package io.izzel.arclight.common.mixin.core.world;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeMap;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
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
import net.minecraft.util.math.Vec3d;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Mixin(TrackedEntity.class)
public abstract class TrackedEntityMixin {

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
    @Shadow private Vec3d velocity;
    @Shadow private int encodedRotationYawHead;
    @Shadow protected abstract void sendPacket(IPacket<?> p_219451_1_);
    // @formatter:on

    private Set<ServerPlayerEntity> trackedPlayers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(ServerWorld p_i50704_1_, Entity p_i50704_2_, int p_i50704_3_, boolean p_i50704_4_, Consumer<IPacket<?>> p_i50704_5_, CallbackInfo ci) {
        trackedPlayers = new HashSet<>();
    }

    public void arclight$constructor(ServerWorld p_i50704_1_, Entity p_i50704_2_, int p_i50704_3_, boolean p_i50704_4_, Consumer<IPacket<?>> p_i50704_5_) {
        throw new NullPointerException();
    }

    public void arclight$constructor(ServerWorld p_i50704_1_, Entity p_i50704_2_, int p_i50704_3_, boolean p_i50704_4_, Consumer<IPacket<?>> p_i50704_5_, Set<ServerPlayerEntity> set) {
        arclight$constructor(p_i50704_1_, p_i50704_2_, p_i50704_3_, p_i50704_4_, p_i50704_5_);
        this.trackedPlayers = set;
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
            ItemFrameEntity entityitemframe = (ItemFrameEntity) this.trackedEntity;
            ItemStack itemstack = entityitemframe.getDisplayedItem();
            if (this.updateCounter % 10 == 0 && itemstack.getItem() instanceof FilledMapItem) {
                MapData worldmap = FilledMapItem.getMapData(itemstack, this.world);
                for (ServerPlayerEntity entityplayer : this.trackedPlayers) {
                    worldmap.updateVisiblePlayers(entityplayer, itemstack);
                    IPacket<?> packet = ((FilledMapItem) itemstack.getItem()).getUpdatePacket(itemstack, this.world, entityplayer);
                    if (packet != null) {
                        entityplayer.connection.sendPacket(packet);
                    }
                }
            }
            this.sendMetadata();
        }
        if (this.updateCounter % this.updateFrequency == 0 || this.trackedEntity.isAirBorne || this.trackedEntity.getDataManager().isDirty()) {
            if (this.trackedEntity.isPassenger()) {
                int i = MathHelper.floor(this.trackedEntity.rotationYaw * 256.0f / 360.0f);
                int j = MathHelper.floor(this.trackedEntity.rotationPitch * 256.0f / 360.0f);
                boolean flag = Math.abs(i - this.encodedRotationYaw) >= 1 || Math.abs(j - this.encodedRotationPitch) >= 1;
                if (flag) {
                    this.packetConsumer.accept(new SEntityPacket.LookPacket(this.trackedEntity.getEntityId(), (byte) i, (byte) j, this.trackedEntity.onGround));
                    this.encodedRotationYaw = i;
                    this.encodedRotationPitch = j;
                }
                this.updateEncodedPosition();
                this.sendMetadata();
                this.riding = true;
            } else {
                ++this.ticksSinceAbsoluteTeleport;
                int i = MathHelper.floor(this.trackedEntity.rotationYaw * 256.0f / 360.0f);
                int j = MathHelper.floor(this.trackedEntity.rotationPitch * 256.0f / 360.0f);
                Vec3d vec3d = this.trackedEntity.getPositionVec().subtract(SEntityPacket.func_218744_a(this.encodedPosX, this.encodedPosY, this.encodedPosZ));
                boolean flag2 = vec3d.lengthSquared() >= 7.62939453125E-6;
                IPacket<?> packet2 = null;
                boolean flag3 = flag2 || this.updateCounter % 60 == 0;
                boolean flag4 = Math.abs(i - this.encodedRotationYaw) >= 1 || Math.abs(j - this.encodedRotationPitch) >= 1;
                if (flag3) {
                    this.updateEncodedPosition();
                }
                if (flag4) {
                    this.encodedRotationYaw = i;
                    this.encodedRotationPitch = j;
                }
                if (this.updateCounter > 0 || this.trackedEntity instanceof AbstractArrowEntity) {
                    long k = SEntityPacket.func_218743_a(vec3d.x);
                    long l = SEntityPacket.func_218743_a(vec3d.y);
                    long i2 = SEntityPacket.func_218743_a(vec3d.z);
                    boolean flag5 = k < -32768L || k > 32767L || l < -32768L || l > 32767L || i2 < -32768L || i2 > 32767L;
                    if (!flag5 && this.ticksSinceAbsoluteTeleport <= 400 && !this.riding && this.onGround == this.trackedEntity.onGround) {
                        if ((!flag3 || !flag4) && !(this.trackedEntity instanceof AbstractArrowEntity)) {
                            if (flag3) {
                                packet2 = new SEntityPacket.RelativeMovePacket(this.trackedEntity.getEntityId(), (short) k, (short) l, (short) i2, this.trackedEntity.onGround);
                            } else if (flag4) {
                                packet2 = new SEntityPacket.LookPacket(this.trackedEntity.getEntityId(), (byte) i, (byte) j, this.trackedEntity.onGround);
                            }
                        } else {
                            packet2 = new SEntityPacket.MovePacket(this.trackedEntity.getEntityId(), (short) k, (short) l, (short) i2, (byte) i, (byte) j, this.trackedEntity.onGround);
                        }
                    } else {
                        this.onGround = this.trackedEntity.onGround;
                        this.ticksSinceAbsoluteTeleport = 0;
                        packet2 = new SEntityTeleportPacket(this.trackedEntity);
                    }
                }
                if ((this.sendVelocityUpdates || this.trackedEntity.isAirBorne || (this.trackedEntity instanceof LivingEntity && ((LivingEntity) this.trackedEntity).isElytraFlying())) && this.updateCounter > 0) {
                    Vec3d vec3d2 = this.trackedEntity.getMotion();
                    double d0 = vec3d2.squareDistanceTo(this.velocity);
                    if (d0 > 1.0E-7 || (d0 > 0.0 && vec3d2.lengthSquared() == 0.0)) {
                        this.velocity = vec3d2;
                        this.packetConsumer.accept(new SEntityVelocityPacket(this.trackedEntity.getEntityId(), this.velocity));
                    }
                }
                if (packet2 != null) {
                    this.packetConsumer.accept(packet2);
                }
                this.sendMetadata();
                this.riding = false;
            }
            int i = MathHelper.floor(this.trackedEntity.getRotationYawHead() * 256.0f / 360.0f);
            if (Math.abs(i - this.encodedRotationYawHead) >= 1) {
                this.packetConsumer.accept(new SEntityHeadLookPacket(this.trackedEntity, (byte) i));
                this.encodedRotationYawHead = i;
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

    @Redirect(method = "track", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/TrackedEntity;sendSpawnPackets(Ljava/util/function/Consumer;)V"))
    private void arclight$addPlayer(TrackedEntity trackedEntity, Consumer<IPacket<?>> consumer, ServerPlayerEntity playerEntity) {
        a(consumer, playerEntity);
    }

    public void a(final Consumer<IPacket<?>> consumer, final ServerPlayerEntity entityplayer) {
        if (this.trackedEntity.removed) {
            return;
        }
        final IPacket<?> packet = this.trackedEntity.createSpawnPacket();
        this.encodedRotationYawHead = MathHelper.floor(this.trackedEntity.getRotationYawHead() * 256.0f / 360.0f);
        consumer.accept(packet);
        if (!this.trackedEntity.getDataManager().isEmpty()) {
            consumer.accept(new SEntityMetadataPacket(this.trackedEntity.getEntityId(), this.trackedEntity.getDataManager(), true));
        }
        boolean flag = this.sendVelocityUpdates;
        if (this.trackedEntity instanceof LivingEntity) {
            final AttributeMap attributemapserver = (AttributeMap) ((LivingEntity) this.trackedEntity).getAttributes();
            final Collection<IAttributeInstance> collection = attributemapserver.getWatchedAttributes();
            if (this.trackedEntity.getEntityId() == entityplayer.getEntityId()) {
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
            for (final EquipmentSlotType enumitemslot : EquipmentSlotType.values()) {
                final ItemStack itemstack = ((LivingEntity) this.trackedEntity).getItemStackFromSlot(enumitemslot);
                if (!itemstack.isEmpty()) {
                    consumer.accept(new SEntityEquipmentPacket(this.trackedEntity.getEntityId(), enumitemslot, itemstack));
                }
            }
        }
        this.encodedRotationYawHead = MathHelper.floor(this.trackedEntity.getRotationYawHead() * 256.0f / 360.0f);
        consumer.accept(new SEntityHeadLookPacket(this.trackedEntity, (byte) this.encodedRotationYawHead));
        if (this.trackedEntity instanceof LivingEntity) {
            final LivingEntity entityliving = (LivingEntity) this.trackedEntity;
            for (final EffectInstance mobeffect : entityliving.getActivePotionEffects()) {
                consumer.accept(new SPlayEntityEffectPacket(this.trackedEntity.getEntityId(), mobeffect));
            }
        }
        if (!this.trackedEntity.getPassengers().isEmpty()) {
            consumer.accept(new SSetPassengersPacket(this.trackedEntity));
        }
        if (this.trackedEntity.isPassenger()) {
            consumer.accept(new SSetPassengersPacket(this.trackedEntity.getRidingEntity()));
        }
        if (this.trackedEntity instanceof MobEntity) {
            MobEntity mobentity = (MobEntity) this.trackedEntity;
            if (mobentity.getLeashed()) {
                consumer.accept(new SMountEntityPacket(mobentity, mobentity.getLeashHolder()));
            }
        }
    }

    @Inject(method = "sendMetadata", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/TrackedEntity;sendPacket(Lnet/minecraft/network/IPacket;)V"))
    private void arclight$sendScaledHealth(CallbackInfo ci, EntityDataManager entitydatamanager, AttributeMap attributemap, Set<IAttributeInstance> set) {
        if (this.trackedEntity instanceof ServerPlayerEntity) {
            ((ServerPlayerEntityBridge) this.trackedEntity).bridge$getBukkitEntity().injectScaledMaxHealth(set, false);
        }
    }
}
