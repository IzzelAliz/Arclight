package io.izzel.arclight.common.mixin.core.world.entity.player;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.util.DamageSourceBridge;
import io.izzel.arclight.common.bridge.core.util.FoodStatsBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.server.ServerWorldBridge;
import io.izzel.arclight.common.mixin.core.world.entity.LivingEntityMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.common.ForgeHooks;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.util.CraftVector;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.spigotmc.SpigotWorldConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(net.minecraft.world.entity.player.Player.class)
public abstract class PlayerMixin extends LivingEntityMixin implements PlayerEntityBridge {

    // @formatter:off
    @Shadow public abstract String getScoreboardName();
    @Shadow @Final private Abilities abilities;
    @Shadow public abstract float getAttackStrengthScale(float adjustTicks);
    @Shadow public abstract void resetAttackStrengthTicker();
    @Shadow public abstract SoundSource getSoundSource();
    @Shadow public abstract float getSpeed();
    @Shadow public abstract void sweepAttack();
    @Shadow public abstract void crit(Entity entityHit);
    @Shadow public abstract void magicCrit(Entity entityHit);
    @Shadow public abstract void awardStat(ResourceLocation p_195067_1_, int p_195067_2_);
    @Shadow public abstract void causeFoodExhaustion(float exhaustion);
    @Shadow private long timeEntitySatOnShoulder;
    @Shadow public abstract void setShoulderEntityRight(CompoundTag tag);
    @Shadow public abstract void setShoulderEntityLeft(CompoundTag tag);
    @Shadow public abstract CompoundTag getShoulderEntityRight();
    @Shadow public abstract CompoundTag getShoulderEntityLeft();
    @Shadow public int experienceLevel;
    @Shadow @Final private Inventory inventory;
    @Shadow public AbstractContainerMenu containerMenu;
    @Shadow @Final public InventoryMenu inventoryMenu;
    @Shadow public abstract void awardStat(Stat<?> stat);
    @Shadow public abstract void awardStat(ResourceLocation stat);
    @Shadow public abstract Component getDisplayName();
    @Shadow public abstract HumanoidArm getMainArm();
    @Shadow public float experienceProgress;
    @Shadow public int totalExperience;
    @Shadow protected FoodData foodData;
    @Shadow protected boolean isImmobile() { return false; }
    @Shadow public abstract Scoreboard getScoreboard();
    @Shadow protected PlayerEnderChestContainer enderChestInventory;
    @Shadow public abstract Either<net.minecraft.world.entity.player.Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos at);
    @Shadow public int sleepCounter;
    @Shadow public abstract GameProfile getGameProfile();
    @Shadow public abstract Inventory getInventory();
    @Shadow public abstract Abilities getAbilities();
    // @formatter:on

    public boolean fauxSleeping;
    public int oldLevel;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(Level p_i241920_1_, BlockPos p_i241920_2_, float p_i241920_3_, GameProfile p_i241920_4_, CallbackInfo ci) {
        oldLevel = -1;
        ((FoodStatsBridge) this.foodData).bridge$setEntityHuman((net.minecraft.world.entity.player.Player) (Object) this);
        ((IInventoryBridge) this.enderChestInventory).setOwner(this.getBukkitEntity());
    }

    @Override
    public boolean bridge$isFauxSleeping() {
        return fauxSleeping;
    }

    @Inject(method = "turtleHelmetTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private void arclight$turtleHelmet(CallbackInfo ci) {
        bridge$pushEffectCause(EntityPotionEffectEvent.Cause.TURTLE_HELMET);
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private void arclight$healByRegen(CallbackInfo ci) {
        bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.REGEN);
    }

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
        cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "RETURN", ordinal = 1))
    private void arclight$playerDropItem(ItemStack droppedItem, boolean dropAround, boolean traceItem, CallbackInfoReturnable<ItemEntity> cir, double d0, ItemEntity itemEntity) {
        Player player = (Player) this.getBukkitEntity();
        Item drop = (Item) ((EntityBridge) itemEntity).bridge$getBukkitEntity();

        PlayerDropItemEvent event = new PlayerDropItemEvent(player, drop);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            org.bukkit.inventory.ItemStack cur = player.getInventory().getItemInHand();
            if (traceItem && (cur == null || cur.getAmount() == 0)) {
                // The complete stack was dropped
                player.getInventory().setItemInHand(drop.getItemStack());
            } else if (traceItem && cur.isSimilar(drop.getItemStack()) && cur.getAmount() < cur.getMaxStackSize() && drop.getItemStack().getAmount() == 1) {
                // Only one item is dropped
                cur.setAmount(cur.getAmount() + 1);
                player.getInventory().setItemInHand(cur);
            } else {
                // Fallback
                player.getInventory().addItem(drop.getItemStack());
            }
            cir.setReturnValue(null);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean hurt(DamageSource source, float amount) {
        if (!ForgeHooks.onPlayerAttack((net.minecraft.world.entity.player.Player) (Object) this, source, amount))
            return false;
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (this.abilities.invulnerable && !source.isBypassInvul()) {
            this.forceExplosionKnockback = true;
            return false;
        } else {
            this.noActionTime = 0;
            if (this.getHealth() <= 0.0F) {
                return false;
            } else {
                if (source.scalesWithDifficulty()) {
                    if (this.level.getDifficulty() == Difficulty.PEACEFUL) {
                        // amount = 0.0F;
                        return false;
                    }

                    if (this.level.getDifficulty() == Difficulty.EASY) {
                        amount = Math.min(amount / 2.0F + 1.0F, amount);
                    }

                    if (this.level.getDifficulty() == Difficulty.HARD) {
                        amount = amount * 3.0F / 2.0F;
                    }
                }

                boolean damaged = super.hurt(source, amount);
                if (damaged) {
                    this.removeEntitiesOnShoulder();
                }
                return damaged;
                //return amount == 0.0F ? false : super.attackEntityFrom(source, amount);
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean canHarmPlayer(final net.minecraft.world.entity.player.Player entityhuman) {
        Team team;
        if (entityhuman instanceof ServerPlayer) {
            final ServerPlayer thatPlayer = (ServerPlayer) entityhuman;
            team = ((ServerPlayerEntityBridge) thatPlayer).bridge$getBukkitEntity().getScoreboard().getPlayerTeam(((ServerPlayerEntityBridge) thatPlayer).bridge$getBukkitEntity());
            if (team == null || team.allowFriendlyFire()) {
                return true;
            }
        } else {
            final OfflinePlayer thisPlayer = Bukkit.getOfflinePlayer(entityhuman.getScoreboardName());
            team = Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(thisPlayer);
            if (team == null || team.allowFriendlyFire()) {
                return true;
            }
        }
        if ((Object) this instanceof ServerPlayer) {
            return !team.hasPlayer(((ServerPlayerEntityBridge) this).bridge$getBukkitEntity());
        }
        return !team.hasPlayer(Bukkit.getOfflinePlayer(this.getScoreboardName()));
    }

    @Inject(method = "actuallyHurt", cancellable = true, at = @At("HEAD"))
    private void arclight$damageEntityCustom(DamageSource damageSrc, float damageAmount, CallbackInfo ci) {
        damageEntity0(damageSrc, damageAmount);
        ci.cancel();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void attack(final Entity entity) {
        if (!net.minecraftforge.common.ForgeHooks.onPlayerAttackTarget((net.minecraft.world.entity.player.Player) (Object) this, entity))
            return;
        if (entity.isAttackable() && !entity.skipAttackInteraction((net.minecraft.world.entity.player.Player) (Object) this)) {
            float f = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float f2;
            if (entity instanceof LivingEntity) {
                f2 = EnchantmentHelper.getDamageBonus(this.getMainHandItem(), ((LivingEntity) entity).getMobType());
            } else {
                f2 = EnchantmentHelper.getDamageBonus(this.getMainHandItem(), MobType.UNDEFINED);
            }
            final float f3 = this.getAttackStrengthScale(0.5f);
            f *= 0.2f + f3 * f3 * 0.8f;
            f2 *= f3;
            // this.resetAttackStrengthTicker();
            if (f > 0.0f || f2 > 0.0f) {
                final boolean flag = f3 > 0.9f;
                boolean flag2 = false;
                float i = (float) this.getAttributeValue(Attributes.ATTACK_KNOCKBACK); // Forge: Initialize this value to the attack knockback attribute of the player, which is by default 0
                i += EnchantmentHelper.getKnockbackBonus((net.minecraft.world.entity.player.Player) (Object) this);
                if (this.isSprinting() && flag) {
                    this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, this.getSoundSource(), 1.0f, 1.0f);
                    ++i;
                    flag2 = true;
                }
                boolean flag3 = flag && this.fallDistance > 0.0f && !this.onGround && !this.onClimbable() && !this.isInWater() && !this.hasEffect(MobEffects.BLINDNESS) && !this.isPassenger() && entity instanceof LivingEntity;
                flag3 = flag3 && !this.isSprinting();
                net.minecraftforge.event.entity.player.CriticalHitEvent hitResult = net.minecraftforge.common.ForgeHooks.getCriticalHit((net.minecraft.world.entity.player.Player) (Object) this, entity, flag3, flag3 ? 1.5F : 1.0F);
                flag3 = hitResult != null;
                if (flag3) {
                    f *= hitResult.getDamageModifier();
                }
                f += f2;
                boolean flag4 = false;
                final double d0 = this.walkDist - this.walkDistO;
                if (flag && !flag3 && !flag2 && this.onGround && d0 < this.getSpeed()) {
                    final ItemStack itemstack = this.getItemInHand(InteractionHand.MAIN_HAND);
                    flag4 = itemstack.canPerformAction(net.minecraftforge.common.ToolActions.SWORD_SWEEP);
                }
                float f4 = 0.0f;
                boolean flag5 = false;
                final int j = EnchantmentHelper.getFireAspect((net.minecraft.world.entity.player.Player) (Object) this);
                if (entity instanceof LivingEntity) {
                    f4 = ((LivingEntity) entity).getHealth();
                    if (j > 0 && !entity.isOnFire()) {
                        final EntityCombustByEntityEvent combustEvent = new EntityCombustByEntityEvent(this.getBukkitEntity(), ((EntityBridge) entity).bridge$getBukkitEntity(), 1);
                        Bukkit.getPluginManager().callEvent(combustEvent);
                        if (!combustEvent.isCancelled()) {
                            flag5 = true;
                            ((EntityBridge) entity).bridge$setOnFire(combustEvent.getDuration(), false);
                        }
                    }
                }
                final Vec3 vec3d = entity.getDeltaMovement();
                final boolean flag6 = entity.hurt(DamageSource.playerAttack((net.minecraft.world.entity.player.Player) (Object) this), f);
                if (flag6) {
                    if (i > 0) {
                        if (entity instanceof LivingEntity) {
                            ((LivingEntity) entity).knockback(i * 0.5f, Mth.sin(this.getYRot() * 0.017453292f), -Mth.cos(this.getYRot() * 0.017453292f));
                        } else {
                            entity.push(-Mth.sin(this.getYRot() * 0.017453292f) * i * 0.5f, 0.1, Mth.cos(this.getYRot() * 0.017453292f) * i * 0.5f);
                        }
                        this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
                        this.setSprinting(false);
                    }
                    if (flag4) {
                        final float f5 = 1.0f + EnchantmentHelper.getSweepingDamageRatio((net.minecraft.world.entity.player.Player) (Object) this) * f;
                        final List<LivingEntity> list = this.level.getEntitiesOfClass(LivingEntity.class, this.getItemInHand(InteractionHand.MAIN_HAND).getSweepHitBox((net.minecraft.world.entity.player.Player) (Object) this, entity));
                        for (final LivingEntity entityliving : list) {
                            if (entityliving != (Object) this && entityliving != entity && !this.isAlliedTo(entityliving) && (!(entityliving instanceof ArmorStand) || !((ArmorStand) entityliving).isMarker()) && this.distanceToSqr(entityliving) < 9.0 && entityliving.hurt(((DamageSourceBridge) DamageSource.playerAttack((net.minecraft.world.entity.player.Player) (Object) this)).bridge$sweep(), f5)) {
                                entityliving.knockback(0.4f, Mth.sin(this.getYRot() * 0.017453292f), -Mth.cos(this.getYRot() * 0.017453292f));
                            }
                        }
                        this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, this.getSoundSource(), 1.0f, 1.0f);
                        this.sweepAttack();
                    }
                    if (entity instanceof ServerPlayer && entity.hurtMarked) {
                        boolean cancelled = false;
                        final Player player = ((ServerPlayerEntityBridge) entity).bridge$getBukkitEntity();
                        final Vector velocity = CraftVector.toBukkit(vec3d);
                        final PlayerVelocityEvent event = new PlayerVelocityEvent(player, velocity.clone());
                        Bukkit.getPluginManager().callEvent(event);
                        if (event.isCancelled()) {
                            cancelled = true;
                        } else if (!velocity.equals(event.getVelocity())) {
                            player.setVelocity(event.getVelocity());
                        }
                        if (!cancelled) {
                            ((ServerPlayer) entity).connection.send(new ClientboundSetEntityMotionPacket(entity));
                            entity.hurtMarked = false;
                            entity.setDeltaMovement(vec3d);
                        }
                    }
                    if (flag3) {
                        this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, this.getSoundSource(), 1.0f, 1.0f);
                        this.crit(entity);
                    }
                    if (!flag3 && !flag4) {
                        if (flag) {
                            this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, this.getSoundSource(), 1.0f, 1.0f);
                        } else {
                            this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_WEAK, this.getSoundSource(), 1.0f, 1.0f);
                        }
                    }
                    if (f2 > 0.0f) {
                        this.magicCrit(entity);
                    }
                    this.setLastHurtMob(entity);
                    if (entity instanceof LivingEntity) {
                        EnchantmentHelper.doPostHurtEffects((LivingEntity) entity, (net.minecraft.world.entity.player.Player) (Object) this);
                    }
                    EnchantmentHelper.doPostDamageEffects((net.minecraft.world.entity.player.Player) (Object) this, entity);
                    final ItemStack itemstack2 = this.getMainHandItem();
                    Object object = entity;
                    if (entity instanceof EnderDragonPart) {
                        object = ((EnderDragonPart) entity).parentMob;
                    }
                    if (!this.level.isClientSide && !itemstack2.isEmpty() && object instanceof LivingEntity) {
                        ItemStack copy = itemstack2.copy();
                        itemstack2.hurtEnemy((LivingEntity) object, (net.minecraft.world.entity.player.Player) (Object) this);
                        if (itemstack2.isEmpty()) {
                            net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem((net.minecraft.world.entity.player.Player) (Object) this, copy, InteractionHand.MAIN_HAND);
                            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        }
                    }
                    if (entity instanceof LivingEntity) {
                        final float f6 = f4 - ((LivingEntity) entity).getHealth();
                        this.awardStat(Stats.DAMAGE_DEALT, Math.round(f6 * 10.0f));
                        if (j > 0) {
                            final EntityCombustByEntityEvent combustEvent2 = new EntityCombustByEntityEvent(this.getBukkitEntity(), ((EntityBridge) entity).bridge$getBukkitEntity(), j * 4);
                            Bukkit.getPluginManager().callEvent(combustEvent2);
                            if (!combustEvent2.isCancelled()) {
                                ((EntityBridge) entity).bridge$setOnFire(combustEvent2.getDuration(), false);
                            }
                        }
                        if (this.level instanceof ServerLevel && f6 > 2.0f) {
                            final int k = (int) (f6 * 0.5);
                            ((ServerLevel) this.level).sendParticles(ParticleTypes.DAMAGE_INDICATOR, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5f, entity.getZ(), k, 0.1, 0.0, 0.1, 0.2);
                        }
                    }
                    bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.ATTACK);
                    this.causeFoodExhaustion(((WorldBridge) level).bridge$spigotConfig().combatExhaustion);
                } else {
                    this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource(), 1.0f, 1.0f);
                    if (flag5) {
                        entity.clearFire();
                    }
                    if (this instanceof ServerPlayerEntityBridge) {
                        ((ServerPlayerEntityBridge) this).bridge$getBukkitEntity().updateInventory();
                    }
                }
            }
        }
    }

    protected transient boolean arclight$forceSleep;

    public Either<net.minecraft.world.entity.player.Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos at, boolean force) {
        this.arclight$forceSleep = force;
        return this.startSleepInBed(at);
    }

    @Override
    public Either<net.minecraft.world.entity.player.Player.BedSleepingProblem, Unit> bridge$trySleep(BlockPos at, boolean force) {
        return startSleepInBed(at, force);
    }

    @Inject(method = "stopSleepInBed", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;sleepCounter:I"))
    private void arclight$wakeup(boolean flag, boolean flag1, CallbackInfo ci) {
        BlockPos blockPos = this.getSleepingPos().orElse(null);
        if (this.bridge$getBukkitEntity() instanceof Player player) {
            Block bed;
            if (blockPos != null) {
                bed = CraftBlock.at(this.level, blockPos);
            } else {
                bed = ((WorldBridge) this.level).bridge$getWorld().getBlockAt(player.getLocation());
            }
            PlayerBedLeaveEvent event = new PlayerBedLeaveEvent(player, bed, true);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    @Redirect(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void arclight$exhaustInfo(net.minecraft.world.entity.player.Player player, float f) {
        SpigotWorldConfig config = ((WorldBridge) level).bridge$spigotConfig();
        if (config != null) {
            if (this.isSprinting()) {
                f = config.jumpSprintExhaustion;
                bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.JUMP_SPRINT);
            } else {
                f = config.jumpWalkExhaustion;
                bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.JUMP);
            }
        }
        this.causeFoodExhaustion(f);
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSharedFlag(IZ)V"))
    private void arclight$toggleGlide(net.minecraft.world.entity.player.Player playerEntity, int flag, boolean set) {
        if (playerEntity.getSharedFlag(flag) != set && !CraftEventFactory.callToggleGlideEvent((net.minecraft.world.entity.player.Player) (Object) this, set).isCancelled()) {
            playerEntity.setSharedFlag(flag, set);
        }
    }

    @Inject(method = "checkMovementStatistics", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void arclight$exhauseCause1(double p_36379_, double p_36380_, double p_36381_, CallbackInfo ci) {
        bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.SWIM);
    }

    @Inject(method = "checkMovementStatistics", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void arclight$exhauseCause2(double p_36379_, double p_36380_, double p_36381_, CallbackInfo ci) {
        bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.WALK_UNDERWATER);
    }

    @Inject(method = "checkMovementStatistics", at = @At(value = "INVOKE", ordinal = 2, target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void arclight$exhauseCause3(double p_36379_, double p_36380_, double p_36381_, CallbackInfo ci) {
        bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.WALK_ON_WATER);
    }

    @Inject(method = "checkMovementStatistics", at = @At(value = "INVOKE", ordinal = 3, target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void arclight$exhauseCause4(double p_36379_, double p_36380_, double p_36381_, CallbackInfo ci) {
        bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.SPRINT);
    }

    @Inject(method = "checkMovementStatistics", at = @At(value = "INVOKE", ordinal = 4, target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void arclight$exhauseCause5(double p_36379_, double p_36380_, double p_36381_, CallbackInfo ci) {
        bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.CROUCH);
    }

    @Inject(method = "checkMovementStatistics", at = @At(value = "INVOKE", ordinal = 5, target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void arclight$exhauseCause6(double p_36379_, double p_36380_, double p_36381_, CallbackInfo ci) {
        bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.WALK);
    }

    @Inject(method = "startFallFlying", cancellable = true, at = @At("HEAD"))
    private void arclight$startGlidingEvent(CallbackInfo ci) {
        if (CraftEventFactory.callToggleGlideEvent((net.minecraft.world.entity.player.Player) (Object) this, true).isCancelled()) {
            this.setSharedFlag(7, true);
            this.setSharedFlag(7, false);
            ci.cancel();
        }
    }

    @Inject(method = "stopFallFlying", cancellable = true, at = @At("HEAD"))
    private void arclight$stopGlidingEvent(CallbackInfo ci) {
        if (CraftEventFactory.callToggleGlideEvent((net.minecraft.world.entity.player.Player) (Object) this, false).isCancelled()) {
            ci.cancel();
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void removeEntitiesOnShoulder() {
        if (this.timeEntitySatOnShoulder + 20L < this.level.getGameTime()) {
            if (this.spawnEntityFromShoulder(this.getShoulderEntityLeft())) {
                this.setShoulderEntityLeft(new CompoundTag());
            }
            if (this.spawnEntityFromShoulder(this.getShoulderEntityRight())) {
                this.setShoulderEntityRight(new CompoundTag());
            }
        }
    }

    private boolean spawnEntityFromShoulder(final CompoundTag nbttagcompound) {
        return this.level.isClientSide || nbttagcompound.isEmpty() || EntityType.create(nbttagcompound, this.level).map(entity -> {
            if (entity instanceof TamableAnimal) {
                ((TamableAnimal) entity).setOwnerUUID(this.uuid);
            }
            entity.setPos(this.getX(), this.getY() + 0.699999988079071, this.getZ());
            return ((ServerWorldBridge) this.level).bridge$addEntitySerialized(entity, CreatureSpawnEvent.SpawnReason.SHOULDER_ENTITY);
        }).orElse(true);
    }

    public CraftHumanEntity getBukkitEntity() {
        return (CraftHumanEntity) ((InternalEntityBridge) this).internal$getBukkitEntity();
    }

    @Override
    public CraftHumanEntity bridge$getBukkitEntity() {
        return (CraftHumanEntity) ((InternalEntityBridge) this).internal$getBukkitEntity();
    }

    @Override
    public void setItemSlot(EquipmentSlot slotIn, ItemStack stack, boolean silent) {
        if (slotIn == EquipmentSlot.MAINHAND) {
            this.bridge$playEquipSound(stack, silent);
            this.inventory.items.set(this.inventory.selected, stack);
        } else if (slotIn == EquipmentSlot.OFFHAND) {
            this.bridge$playEquipSound(stack, silent);
            this.inventory.offhand.set(0, stack);
        } else if (slotIn.getType() == EquipmentSlot.Type.ARMOR) {
            this.bridge$playEquipSound(stack, silent);
            this.inventory.armor.set(slotIn.getIndex(), stack);
        }
    }

    @Redirect(method = "causeFoodExhaustion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V"))
    private void arclight$exhaustEvent(FoodData foodData, float amount) {
        EntityExhaustionEvent.ExhaustionReason reason = arclight$exhaustReason == null ? EntityExhaustionEvent.ExhaustionReason.UNKNOWN : arclight$exhaustReason;
        arclight$exhaustReason = null;
        EntityExhaustionEvent event = CraftEventFactory.callPlayerExhaustionEvent((net.minecraft.world.entity.player.Player) (Object) this, reason, amount);
        if (!event.isCancelled()) {
            this.foodData.addExhaustion(event.getExhaustion());
        }
    }

    private EntityExhaustionEvent.ExhaustionReason arclight$exhaustReason;

    public void applyExhaustion(float f, EntityExhaustionEvent.ExhaustionReason reason) {
        bridge$pushExhaustReason(reason);
        this.causeFoodExhaustion(f);
    }

    @Override
    public void bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason reason) {
        arclight$exhaustReason = reason;
    }
}
