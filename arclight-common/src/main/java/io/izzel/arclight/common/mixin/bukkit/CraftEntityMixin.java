package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.mod.server.entity.ArclightFakePlayer;
import io.izzel.arclight.common.mod.server.entity.ArclightModChestedHorse;
import io.izzel.arclight.common.mod.server.entity.ArclightModEntity;
import io.izzel.arclight.common.mod.server.entity.ArclightModHorse;
import io.izzel.arclight.common.mod.server.entity.ArclightModLivingEntity;
import io.izzel.arclight.common.mod.server.entity.ArclightModMinecart;
import io.izzel.arclight.common.mod.server.entity.ArclightModMinecartContainer;
import io.izzel.arclight.common.mod.server.entity.ArclightModMob;
import io.izzel.arclight.common.mod.server.entity.ArclightModProjectile;
import io.izzel.arclight.common.mod.server.entity.ArclightModRaider;
import io.izzel.arclight.common.mod.util.ResourceLocationUtil;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FlyingEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.minecart.AbstractMinecartEntity;
import net.minecraft.entity.item.minecart.ContainerMinecartEntity;
import net.minecraft.entity.monster.AbstractRaiderEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.horse.AbstractChestedHorseEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftAgeable;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.craftbukkit.v.entity.CraftFlying;
import org.bukkit.craftbukkit.v.entity.CraftGolem;
import org.bukkit.craftbukkit.v.entity.CraftTameableAnimal;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;
import java.util.Objects;

@Mixin(value = CraftEntity.class, remap = false)
public abstract class CraftEntityMixin implements org.bukkit.entity.Entity {

    @Shadow protected Entity entity;

    private EntityType arclight$type;

    @Override
    public @NotNull EntityType getType() {
        if (this.arclight$type == null) {
            ResourceLocation location = ForgeRegistries.ENTITIES.getKey(entity.getType());
            if (location == null) throw new IllegalArgumentException("Unregistered entity type " + entity.getType());
            if (location.getNamespace().equals("minecraft")) {
                this.arclight$type = Objects.requireNonNull(EntityType.fromName(location.getPath().toUpperCase(Locale.ROOT)));
            } else {
                this.arclight$type = EntityType.valueOf(ResourceLocationUtil.standardize(location));
            }
        }
        return this.arclight$type;
    }

    @Inject(method = "getEntity", cancellable = true, at = @At("HEAD"))
    private static void arclight$fakePlayer(CraftServer server, Entity entity, CallbackInfoReturnable<CraftEntity> cir) {
        if (entity instanceof FakePlayer) {
            cir.setReturnValue(new ArclightFakePlayer(server, (FakePlayer) entity));
        }
    }

    @Inject(method = "getEntity", cancellable = true, at = @At(value = "NEW", target = "java/lang/AssertionError"))
    private static void arclight$modEntity(CraftServer server, Entity entity, CallbackInfoReturnable<CraftEntity> cir) {
        if (entity instanceof LivingEntity) {
            if (entity instanceof MobEntity) {
                if (entity instanceof AgeableEntity) {
                    if (entity instanceof AbstractHorseEntity) {
                        if (entity instanceof AbstractChestedHorseEntity) {
                            cir.setReturnValue(new ArclightModChestedHorse(server, (AbstractChestedHorseEntity) entity));
                            return;
                        }
                        cir.setReturnValue(new ArclightModHorse(server, (AbstractHorseEntity) entity));
                        return;
                    }
                    if (entity instanceof TameableEntity) {
                        cir.setReturnValue(new CraftTameableAnimal(server, (TameableEntity) entity));
                        return;
                    }
                    cir.setReturnValue(new CraftAgeable(server, (AgeableEntity) entity));
                    return;
                }
                if (entity instanceof FlyingEntity) {
                    cir.setReturnValue(new CraftFlying(server, (FlyingEntity) entity));
                    return;
                }
                if (entity instanceof AbstractRaiderEntity) {
                    cir.setReturnValue(new ArclightModRaider(server, (AbstractRaiderEntity) entity));
                    return;
                }
                if (entity instanceof GolemEntity) {
                    cir.setReturnValue(new CraftGolem(server, (GolemEntity) entity));
                    return;
                }
                cir.setReturnValue(new ArclightModMob(server, (MobEntity) entity));
                return;
            }
            cir.setReturnValue(new ArclightModLivingEntity(server, (LivingEntity) entity));
            return;
        }
        if (entity instanceof AbstractMinecartEntity) {
            if (entity instanceof ContainerMinecartEntity) {
                cir.setReturnValue(new ArclightModMinecartContainer(server, (ContainerMinecartEntity) entity));
                return;
            }
            cir.setReturnValue(new ArclightModMinecart(server, (AbstractMinecartEntity) entity));
            return;
        }
        if (entity instanceof ProjectileEntity) {
            cir.setReturnValue(new ArclightModProjectile(server, entity));
            return;
        }
        cir.setReturnValue(new ArclightModEntity(server, entity));
    }
}
