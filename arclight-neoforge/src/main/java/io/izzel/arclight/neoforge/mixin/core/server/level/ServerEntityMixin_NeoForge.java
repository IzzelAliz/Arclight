package io.izzel.arclight.neoforge.mixin.core.server.level;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.bundle.PacketAndPayloadAcceptor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin_NeoForge {

    // @formatter:off
    @Shadow @Final private Entity entity;
    @Shadow private int yHeadRotp;
    @Shadow @Nullable private List<SynchedEntityData.DataValue<?>> trackedDataValues;
    @Shadow @Final private boolean trackDelta;
    @Shadow private Vec3 ap;
    // @formatter:on

    /**
     * @author qyl27
     * @reason For bukkit.
     */
    @Overwrite
    public void sendPairingData(ServerPlayer player, final PacketAndPayloadAcceptor<ClientGamePacketListener> packetAndPayloadAcceptor) {
        Mob entityinsentient;
        if (this.entity.isRemoved()) {
            return;
        }
        var packet = this.entity.getAddEntityPacket();
        this.yHeadRotp = Mth.floor(this.entity.getYHeadRot() * 256.0f / 360.0f);
        packetAndPayloadAcceptor.accept(packet);
        if (this.trackedDataValues != null) {
            packetAndPayloadAcceptor.accept(new ClientboundSetEntityDataPacket(this.entity.getId(), this.trackedDataValues));
        }
        boolean flag = this.trackDelta;
        if (this.entity instanceof LivingEntity livingEntity) {
            Collection<AttributeInstance> collection = livingEntity.getAttributes().getSyncableAttributes();
            if (this.entity.getId() == player.getId()) {
                ((ServerPlayerEntityBridge) this.entity).bridge$getBukkitEntity().injectScaledMaxHealth(collection, false);
            }
            if (!collection.isEmpty()) {
                packetAndPayloadAcceptor.accept(new ClientboundUpdateAttributesPacket(this.entity.getId(), collection));
            }
            if (livingEntity.isFallFlying()) {
                flag = true;
            }
        }
        this.ap = this.entity.getDeltaMovement();
        if (flag && !(this.entity instanceof LivingEntity)) {
            packetAndPayloadAcceptor.accept(new ClientboundSetEntityMotionPacket(this.entity.getId(), this.ap));
        }
        if (this.entity instanceof LivingEntity) {
            ArrayList<Pair<EquipmentSlot, ItemStack>> list = Lists.newArrayList();
            for (EquipmentSlot enumitemslot : EquipmentSlot.values()) {
                ItemStack itemstack = ((LivingEntity) this.entity).getItemBySlot(enumitemslot);
                if (itemstack.isEmpty()) continue;
                list.add(Pair.of(enumitemslot, itemstack.copy()));
            }
            if (!list.isEmpty()) {
                packetAndPayloadAcceptor.accept(new ClientboundSetEquipmentPacket(this.entity.getId(), list));
            }
            ((LivingEntity) this.entity).detectEquipmentUpdates();
        }
        // CraftBukkit start - MC-109346: Fix for nonsensical head yaw
        if (this.entity instanceof ServerPlayer) {
            packetAndPayloadAcceptor.accept(new ClientboundRotateHeadPacket(this.entity, (byte) Mth.floor(this.entity.getYHeadRot() * 256.0F / 360.0F)));
        }
        // CraftBukkit end
        if (!this.entity.getPassengers().isEmpty()) {
            packetAndPayloadAcceptor.accept(new ClientboundSetPassengersPacket(this.entity));
        }
        if (this.entity.isPassenger()) {
            packetAndPayloadAcceptor.accept(new ClientboundSetPassengersPacket(this.entity.getVehicle()));
        }
        if (this.entity instanceof Mob && (entityinsentient = (Mob) this.entity).isLeashed()) {
            packetAndPayloadAcceptor.accept(new ClientboundSetEntityLinkPacket(entityinsentient, entityinsentient.getLeashHolder()));
        }
    }
}
