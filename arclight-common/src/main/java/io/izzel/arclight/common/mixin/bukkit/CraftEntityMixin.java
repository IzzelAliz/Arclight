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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
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
import org.spongepowered.asm.mixin.Final;
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
    @Shadow @Final protected CraftServer server;

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
            if (entity instanceof Mob) {
                if (entity instanceof AgeableMob) {
                    if (entity instanceof AbstractHorse) {
                        if (entity instanceof AbstractChestedHorse) {
                            cir.setReturnValue(new ArclightModChestedHorse(server, (AbstractChestedHorse) entity));
                            return;
                        }
                        cir.setReturnValue(new ArclightModHorse(server, (AbstractHorse) entity));
                        return;
                    }
                    if (entity instanceof TamableAnimal) {
                        cir.setReturnValue(new CraftTameableAnimal(server, (TamableAnimal) entity));
                        return;
                    }
                    cir.setReturnValue(new CraftAgeable(server, (AgeableMob) entity));
                    return;
                }
                if (entity instanceof FlyingMob) {
                    cir.setReturnValue(new CraftFlying(server, (FlyingMob) entity));
                    return;
                }
                if (entity instanceof Raider) {
                    cir.setReturnValue(new ArclightModRaider(server, (Raider) entity));
                    return;
                }
                if (entity instanceof AbstractGolem) {
                    cir.setReturnValue(new CraftGolem(server, (AbstractGolem) entity));
                    return;
                }
                cir.setReturnValue(new ArclightModMob(server, (Mob) entity));
                return;
            }
            cir.setReturnValue(new ArclightModLivingEntity(server, (LivingEntity) entity));
            return;
        }
        if (entity instanceof AbstractMinecart) {
            if (entity instanceof AbstractMinecartContainer) {
                cir.setReturnValue(new ArclightModMinecartContainer(server, (AbstractMinecartContainer) entity));
                return;
            }
            cir.setReturnValue(new ArclightModMinecart(server, (AbstractMinecart) entity));
            return;
        }
        if (entity instanceof Projectile) {
            cir.setReturnValue(new ArclightModProjectile(server, entity));
            return;
        }
        cir.setReturnValue(new ArclightModEntity(server, entity));
    }
}
