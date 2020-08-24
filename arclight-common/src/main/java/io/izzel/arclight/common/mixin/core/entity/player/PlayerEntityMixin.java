package io.izzel.arclight.common.mixin.core.entity.player;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.util.DamageSourceBridge;
import io.izzel.arclight.common.bridge.util.FoodStatsBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import io.izzel.arclight.common.mixin.core.entity.LivingEntityMixin;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.CreatureAttribute;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.boss.dragon.EnderDragonPartEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.SEntityVelocityPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effects;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.DamageSource;
import net.minecraft.util.FoodStats;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
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
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
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

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntityMixin implements PlayerEntityBridge {

    // @formatter:off
    @Shadow public abstract String getScoreboardName();
    @Shadow @Final public PlayerAbilities abilities;
    @Shadow public abstract float getCooledAttackStrength(float adjustTicks);
    @Shadow public abstract void resetCooldown();
    @Shadow public abstract SoundCategory getSoundCategory();
    @Shadow public abstract float getAIMoveSpeed();
    @Shadow public abstract void spawnSweepParticles();
    @Shadow public abstract void onCriticalHit(Entity entityHit);
    @Shadow public abstract void onEnchantmentCritical(Entity entityHit);
    @Shadow public abstract void addStat(ResourceLocation p_195067_1_, int p_195067_2_);
    @Shadow public abstract void addExhaustion(float exhaustion);
    @Shadow private long timeEntitySatOnShoulder;
    @Shadow public abstract void setRightShoulderEntity(CompoundNBT tag);
    @Shadow public abstract void setLeftShoulderEntity(CompoundNBT tag);
    @Shadow public abstract CompoundNBT getRightShoulderEntity();
    @Shadow public abstract CompoundNBT getLeftShoulderEntity();
    @Shadow public int experienceLevel;
    @Shadow @Final public PlayerInventory inventory;
    @Shadow public Container openContainer;
    @Shadow @Final public PlayerContainer container;
    @Shadow public abstract void addStat(Stat<?> stat);
    @Shadow public abstract void addStat(ResourceLocation stat);
    @Shadow public abstract ITextComponent getDisplayName();
    @Shadow public abstract HandSide getPrimaryHand();
    @Shadow public float experience;
    @Shadow public int experienceTotal;
    @Shadow protected FoodStats foodStats;
    @Shadow protected boolean isMovementBlocked() { return false; }
    @Shadow public abstract Scoreboard getWorldScoreboard();
    @Shadow protected EnderChestInventory enterChestInventory;
    @Shadow public abstract Either<PlayerEntity.SleepResult, Unit> trySleep(BlockPos at);
    @Shadow public int sleepTimer;
    // @formatter:on

    public boolean fauxSleeping;
    public int oldLevel;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(World p_i241920_1_, BlockPos p_i241920_2_, float p_i241920_3_, GameProfile p_i241920_4_, CallbackInfo ci) {
        oldLevel = -1;
        ((FoodStatsBridge) this.foodStats).bridge$setEntityHuman((PlayerEntity) (Object) this);
        ((IInventoryBridge) this.enterChestInventory).setOwner(this.getBukkitEntity());
    }

    @Override
    public boolean bridge$isFauxSleeping() {
        return fauxSleeping;
    }

    @Inject(method = "updateTurtleHelmet", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$turtleHelmet(CallbackInfo ci) {
        bridge$pushEffectCause(EntityPotionEffectEvent.Cause.TURTLE_HELMET);
    }

    @Inject(method = "livingTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;heal(F)V"))
    private void arclight$healByRegen(CallbackInfo ci) {
        bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.REGEN);
    }

    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/item/ItemEntity;",
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
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (!ForgeHooks.onPlayerAttack((PlayerEntity) (Object) this, source, amount)) return false;
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (this.abilities.disableDamage && !source.canHarmInCreative()) {
            this.forceExplosionKnockback = true;
            return false;
        } else {
            this.idleTime = 0;
            if (this.getHealth() <= 0.0F) {
                return false;
            } else {
                if (source.isDifficultyScaled()) {
                    if (this.world.getDifficulty() == Difficulty.PEACEFUL) {
                        // amount = 0.0F;
                        return false;
                    }

                    if (this.world.getDifficulty() == Difficulty.EASY) {
                        amount = Math.min(amount / 2.0F + 1.0F, amount);
                    }

                    if (this.world.getDifficulty() == Difficulty.HARD) {
                        amount = amount * 3.0F / 2.0F;
                    }
                }

                boolean damaged = super.attackEntityFrom(source, amount);
                if (damaged) {
                    this.spawnShoulderEntities();
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
    public boolean canAttackPlayer(final PlayerEntity entityhuman) {
        Team team;
        if (entityhuman instanceof ServerPlayerEntity) {
            final ServerPlayerEntity thatPlayer = (ServerPlayerEntity) entityhuman;
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
        if ((Object) this instanceof ServerPlayerEntity) {
            return !team.hasPlayer(((ServerPlayerEntityBridge) this).bridge$getBukkitEntity());
        }
        return !team.hasPlayer(Bukkit.getOfflinePlayer(this.getScoreboardName()));
    }

    @Inject(method = "damageEntity", cancellable = true, at = @At("HEAD"))
    private void arclight$damageEntityCustom(DamageSource damageSrc, float damageAmount, CallbackInfo ci) {
        damageEntity0(damageSrc, damageAmount);
        ci.cancel();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void attackTargetEntityWithCurrentItem(final Entity entity) {
        if (!net.minecraftforge.common.ForgeHooks.onPlayerAttackTarget((PlayerEntity) (Object) this, entity)) return;
        if (entity.canBeAttackedWithItem() && !entity.hitByEntity((PlayerEntity) (Object) this)) {
            float f = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float f2;
            if (entity instanceof LivingEntity) {
                f2 = EnchantmentHelper.getModifierForCreature(this.getHeldItemMainhand(), ((LivingEntity) entity).getCreatureAttribute());
            } else {
                f2 = EnchantmentHelper.getModifierForCreature(this.getHeldItemMainhand(), CreatureAttribute.UNDEFINED);
            }
            final float f3 = this.getCooledAttackStrength(0.5f);
            f *= 0.2f + f3 * f3 * 0.8f;
            f2 *= f3;
            this.resetCooldown();
            if (f > 0.0f || f2 > 0.0f) {
                final boolean flag = f3 > 0.9f;
                boolean flag2 = false;
                final byte b0 = 0;
                int i = b0 + EnchantmentHelper.getKnockbackModifier((PlayerEntity) (Object) this);
                if (this.isSprinting() && flag) {
                    this.world.playSound(null, this.getPosX(), this.getPosY(), this.getPosZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, this.getSoundCategory(), 1.0f, 1.0f);
                    ++i;
                    flag2 = true;
                }
                boolean flag3 = flag && this.fallDistance > 0.0f && !this.onGround && !this.isOnLadder() && !this.isInWater() && !this.isPotionActive(Effects.BLINDNESS) && !this.isPassenger() && entity instanceof LivingEntity;
                net.minecraftforge.event.entity.player.CriticalHitEvent hitResult = net.minecraftforge.common.ForgeHooks.getCriticalHit((PlayerEntity) (Object) this, entity, flag3, flag3 ? 1.5F : 1.0F);
                flag3 = hitResult != null;
                if (flag3) {
                    f *= hitResult.getDamageModifier();
                }
                f += f2;
                boolean flag4 = false;
                final double d0 = this.distanceWalkedModified - this.prevDistanceWalkedModified;
                if (flag && !flag3 && !flag2 && this.onGround && d0 < this.getAIMoveSpeed()) {
                    final ItemStack itemstack = this.getHeldItem(Hand.MAIN_HAND);
                    if (itemstack.getItem() instanceof SwordItem) {
                        flag4 = true;
                    }
                }
                float f4 = 0.0f;
                boolean flag5 = false;
                final int j = EnchantmentHelper.getFireAspectModifier((PlayerEntity) (Object) this);
                if (entity instanceof LivingEntity) {
                    f4 = ((LivingEntity) entity).getHealth();
                    if (j > 0 && !entity.isBurning()) {
                        final EntityCombustByEntityEvent combustEvent = new EntityCombustByEntityEvent(this.getBukkitEntity(), ((EntityBridge) entity).bridge$getBukkitEntity(), 1);
                        Bukkit.getPluginManager().callEvent(combustEvent);
                        if (!combustEvent.isCancelled()) {
                            flag5 = true;
                            ((EntityBridge) entity).bridge$setOnFire(combustEvent.getDuration(), false);
                        }
                    }
                }
                final Vector3d vec3d = entity.getMotion();
                final boolean flag6 = entity.attackEntityFrom(DamageSource.causePlayerDamage((PlayerEntity) (Object) this), f);
                if (flag6) {
                    if (i > 0) {
                        if (entity instanceof LivingEntity) {
                            ((LivingEntity) entity).applyKnockback(i * 0.5f, MathHelper.sin(this.rotationYaw * 0.017453292f), -MathHelper.cos(this.rotationYaw * 0.017453292f));
                        } else {
                            entity.addVelocity(-MathHelper.sin(this.rotationYaw * 0.017453292f) * i * 0.5f, 0.1, MathHelper.cos(this.rotationYaw * 0.017453292f) * i * 0.5f);
                        }
                        this.setMotion(this.getMotion().mul(0.6, 1.0, 0.6));
                        this.setSprinting(false);
                    }
                    if (flag4) {
                        final float f5 = 1.0f + EnchantmentHelper.getSweepingDamageRatio((PlayerEntity) (Object) this) * f;
                        final List<LivingEntity> list = this.world.getEntitiesWithinAABB(LivingEntity.class, entity.getBoundingBox().grow(1.0, 0.25, 1.0));
                        for (final LivingEntity entityliving : list) {
                            if (entityliving != (Object) this && entityliving != entity && !this.isOnSameTeam(entityliving) && (!(entityliving instanceof ArmorStandEntity) || !((ArmorStandEntity) entityliving).hasMarker()) && this.getDistanceSq(entityliving) < 9.0 && entityliving.attackEntityFrom(((DamageSourceBridge) DamageSource.causePlayerDamage((PlayerEntity) (Object) this)).bridge$sweep(), f5)) {
                                entityliving.applyKnockback(0.4f, MathHelper.sin(this.rotationYaw * 0.017453292f), -MathHelper.cos(this.rotationYaw * 0.017453292f));
                            }
                        }
                        this.world.playSound(null, this.getPosX(), this.getPosY(), this.getPosZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, this.getSoundCategory(), 1.0f, 1.0f);
                        this.spawnSweepParticles();
                    }
                    if (entity instanceof ServerPlayerEntity && entity.velocityChanged) {
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
                            ((ServerPlayerEntity) entity).connection.sendPacket(new SEntityVelocityPacket(entity));
                            entity.velocityChanged = false;
                            entity.setMotion(vec3d);
                        }
                    }
                    if (flag3) {
                        this.world.playSound(null, this.getPosX(), this.getPosY(), this.getPosZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, this.getSoundCategory(), 1.0f, 1.0f);
                        this.onCriticalHit(entity);
                    }
                    if (!flag3 && !flag4) {
                        if (flag) {
                            this.world.playSound(null, this.getPosX(), this.getPosY(), this.getPosZ(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, this.getSoundCategory(), 1.0f, 1.0f);
                        } else {
                            this.world.playSound(null, this.getPosX(), this.getPosY(), this.getPosZ(), SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, this.getSoundCategory(), 1.0f, 1.0f);
                        }
                    }
                    if (f2 > 0.0f) {
                        this.onEnchantmentCritical(entity);
                    }
                    this.setLastAttackedEntity(entity);
                    if (entity instanceof LivingEntity) {
                        EnchantmentHelper.applyThornEnchantments((LivingEntity) entity, (PlayerEntity) (Object) this);
                    }
                    EnchantmentHelper.applyArthropodEnchantments((PlayerEntity) (Object) this, entity);
                    final ItemStack itemstack2 = this.getHeldItemMainhand();
                    Object object = entity;
                    if (entity instanceof EnderDragonPartEntity) {
                        object = ((EnderDragonPartEntity) entity).dragon;
                    }
                    if (!this.world.isRemote && !itemstack2.isEmpty() && object instanceof LivingEntity) {
                        ItemStack copy = itemstack2.copy();
                        itemstack2.hitEntity((LivingEntity) object, (PlayerEntity) (Object) this);
                        if (itemstack2.isEmpty()) {
                            net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem((PlayerEntity) (Object) this, copy, Hand.MAIN_HAND);
                            this.setHeldItem(Hand.MAIN_HAND, ItemStack.EMPTY);
                        }
                    }
                    if (entity instanceof LivingEntity) {
                        final float f6 = f4 - ((LivingEntity) entity).getHealth();
                        this.addStat(Stats.DAMAGE_DEALT, Math.round(f6 * 10.0f));
                        if (j > 0) {
                            final EntityCombustByEntityEvent combustEvent2 = new EntityCombustByEntityEvent(this.getBukkitEntity(), ((EntityBridge) entity).bridge$getBukkitEntity(), j * 4);
                            Bukkit.getPluginManager().callEvent(combustEvent2);
                            if (!combustEvent2.isCancelled()) {
                                ((EntityBridge) entity).bridge$setOnFire(combustEvent2.getDuration(), false);
                            }
                        }
                        if (this.world instanceof ServerWorld && f6 > 2.0f) {
                            final int k = (int) (f6 * 0.5);
                            ((ServerWorld) this.world).spawnParticle(ParticleTypes.DAMAGE_INDICATOR, entity.getPosX(), entity.getPosY() + entity.getHeight() * 0.5f, entity.getPosZ(), k, 0.1, 0.0, 0.1, 0.2);
                        }
                    }
                    this.addExhaustion(0.1f);
                } else {
                    this.world.playSound(null, this.getPosX(), this.getPosY(), this.getPosZ(), SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, this.getSoundCategory(), 1.0f, 1.0f);
                    if (flag5) {
                        entity.extinguish();
                    }
                    if (this instanceof ServerPlayerEntityBridge) {
                        ((ServerPlayerEntityBridge) this).bridge$getBukkitEntity().updateInventory();
                    }
                }
            }
        }
    }

    protected transient boolean arclight$forceSleep;

    public Either<PlayerEntity.SleepResult, Unit> sleep(BlockPos at, boolean force) {
        this.arclight$forceSleep = force;
        return this.trySleep(at);
    }

    @Override
    public Either<PlayerEntity.SleepResult, Unit> bridge$trySleep(BlockPos at, boolean force) {
        return sleep(at, force);
    }

    @Inject(method = "stopSleepInBed", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;sleepTimer:I"))
    private void arclight$wakeup(boolean flag, boolean flag1, CallbackInfo ci) {
        BlockPos blockPos = this.getBedPosition().orElse(null);
        if (this.bridge$getBukkitEntity() instanceof Player) {
            Player player = (Player) this.bridge$getBukkitEntity();
            Block bed;
            if (blockPos != null) {
                bed = CraftBlock.at(this.world, blockPos);
            } else {
                bed = ((WorldBridge) this.world).bridge$getWorld().getBlockAt(player.getLocation());
            }
            PlayerBedLeaveEvent event = new PlayerBedLeaveEvent(player, bed, true);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setFlag(IZ)V"))
    private void arclight$toggleGlide(PlayerEntity playerEntity, int flag, boolean set) {
        if (playerEntity.getFlag(flag) != set && !CraftEventFactory.callToggleGlideEvent((PlayerEntity) (Object) this, set).isCancelled()) {
            playerEntity.setFlag(flag, set);
        }
    }

    @Inject(method = "startFallFlying", cancellable = true, at = @At("HEAD"))
    private void arclight$startGlidingEvent(CallbackInfo ci) {
        if (CraftEventFactory.callToggleGlideEvent((PlayerEntity) (Object) this, true).isCancelled()) {
            this.setFlag(7, true);
            this.setFlag(7, false);
            ci.cancel();
        }
    }

    @Inject(method = "stopFallFlying", cancellable = true, at = @At("HEAD"))
    private void arclight$stopGlidingEvent(CallbackInfo ci) {
        if (CraftEventFactory.callToggleGlideEvent((PlayerEntity) (Object) this, false).isCancelled()) {
            ci.cancel();
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void spawnShoulderEntities() {
        if (this.timeEntitySatOnShoulder + 20L < this.world.getGameTime()) {
            if (this.spawnEntityFromShoulder(this.getLeftShoulderEntity())) {
                this.setLeftShoulderEntity(new CompoundNBT());
            }
            if (this.spawnEntityFromShoulder(this.getRightShoulderEntity())) {
                this.setRightShoulderEntity(new CompoundNBT());
            }
        }
    }

    private boolean spawnEntityFromShoulder(final CompoundNBT nbttagcompound) {
        return this.world.isRemote || nbttagcompound.isEmpty() || EntityType.loadEntityUnchecked(nbttagcompound, this.world).map(entity -> {
            if (entity instanceof TameableEntity) {
                ((TameableEntity) entity).setOwnerId(this.entityUniqueID);
            }
            entity.setPosition(this.getPosX(), this.getPosY() + 0.699999988079071, this.getPosZ());
            return ((ServerWorldBridge) this.world).bridge$addEntitySerialized(entity, CreatureSpawnEvent.SpawnReason.SHOULDER_ENTITY);
        }).orElse(true);
    }

    public CraftHumanEntity getBukkitEntity() {
        return (CraftHumanEntity) ((InternalEntityBridge) this).internal$getBukkitEntity();
    }

    @Override
    public CraftHumanEntity bridge$getBukkitEntity() {
        return (CraftHumanEntity) ((InternalEntityBridge) this).internal$getBukkitEntity();
    }
}
