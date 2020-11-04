package io.izzel.arclight.common.mixin.core.world;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.world.ExplosionBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.FallingBlockEntity;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.ExplosionContext;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Mixin(Explosion.class)
public abstract class ExplosionMixin implements ExplosionBridge {

    // @formatter:off
    @Shadow @Final private World world;
    @Shadow @Final private Explosion.Mode mode;
    @Shadow @Mutable @Final private float size;
    @Shadow @Final private List<BlockPos> affectedBlockPositions;
    @Shadow @Final private double x;
    @Shadow @Final private double y;
    @Shadow @Final private double z;
    @Shadow @Final public Entity exploder;
    @Shadow public abstract DamageSource getDamageSource();
    @Shadow @Final private Map<PlayerEntity, Vector3d> playerKnockbackMap;
    @Accessor("exploder") public abstract Entity bridge$getExploder();
    @Accessor("size") public abstract float bridge$getSize();
    @Accessor("size") public abstract void bridge$setSize(float size);
    @Accessor("mode") public abstract Explosion.Mode bridge$getMode();
    @Shadow @Final private boolean causesFire;
    @Shadow @Final private Random random;
    @Shadow private static void handleExplosionDrops(ObjectArrayList<Pair<ItemStack, BlockPos>> dropPositionArray, ItemStack stack, BlockPos pos) { }
    // @formatter:on

    @Shadow @Final private ExplosionContext context;

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;DDDFZLnet/minecraft/world/Explosion$Mode;)V",
        at = @At("RETURN"))
    public void arclight$adjustSize(World worldIn, Entity exploderIn, double xIn, double yIn, double zIn, float sizeIn, boolean causesFireIn, Explosion.Mode modeIn, CallbackInfo ci) {
        this.size = Math.max(sizeIn, 0F);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void doExplosionA() {
        if (this.size < 0.1F) {
            return;
        }
        Set<BlockPos> set = Sets.newHashSet();
        int i = 16;

        for (int j = 0; j < 16; ++j) {
            for (int k = 0; k < 16; ++k) {
                for (int l = 0; l < 16; ++l) {
                    if (j == 0 || j == 15 || k == 0 || k == 15 || l == 0 || l == 15) {
                        double d0 = ((float) j / 15.0F * 2.0F - 1.0F);
                        double d1 = ((float) k / 15.0F * 2.0F - 1.0F);
                        double d2 = ((float) l / 15.0F * 2.0F - 1.0F);
                        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                        d0 = d0 / d3;
                        d1 = d1 / d3;
                        d2 = d2 / d3;
                        float f = this.size * (0.7F + this.world.rand.nextFloat() * 0.6F);
                        double d4 = this.x;
                        double d6 = this.y;
                        double d8 = this.z;

                        for (float f1 = 0.3F; f > 0.0F; f -= 0.22500001F) {
                            BlockPos blockpos = new BlockPos(d4, d6, d8);
                            BlockState blockstate = this.world.getBlockState(blockpos);
                            FluidState fluidstate = this.world.getFluidState(blockpos);
                            Optional<Float> optional = this.context.getExplosionResistance((Explosion) (Object) this, this.world, blockpos, blockstate, fluidstate);
                            if (optional.isPresent()) {
                                f -= (optional.get() + 0.3F) * 0.3F;
                            }

                            if (f > 0.0F && this.context.canExplosionDestroyBlock((Explosion) (Object) this, this.world, blockpos, blockstate, f)) {
                                set.add(blockpos);
                            }

                            d4 += d0 * (double) 0.3F;
                            d6 += d1 * (double) 0.3F;
                            d8 += d2 * (double) 0.3F;
                        }
                    }
                }
            }
        }

        this.affectedBlockPositions.addAll(set);
        float f3 = this.size * 2.0F;
        int k1 = MathHelper.floor(this.x - (double) f3 - 1.0D);
        int l1 = MathHelper.floor(this.x + (double) f3 + 1.0D);
        int i2 = MathHelper.floor(this.y - (double) f3 - 1.0D);
        int i1 = MathHelper.floor(this.y + (double) f3 + 1.0D);
        int j2 = MathHelper.floor(this.z - (double) f3 - 1.0D);
        int j1 = MathHelper.floor(this.z + (double) f3 + 1.0D);
        List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this.exploder, new AxisAlignedBB(k1, i2, j2, l1, i1, j1));
        net.minecraftforge.event.ForgeEventFactory.onExplosionDetonate(this.world, (Explosion) (Object) this, list, f3);
        Vector3d vec3d = new Vector3d(this.x, this.y, this.z);

        for (Entity entity : list) {
            if (!entity.isImmuneToExplosions()) {
                double d12 = MathHelper.sqrt(entity.getDistanceSq(vec3d)) / f3;
                if (d12 <= 1.0D) {
                    double d5 = entity.getPosX() - this.x;
                    double d7 = entity.getPosYEye() - this.y;
                    double d9 = entity.getPosZ() - this.z;
                    double d13 = MathHelper.sqrt(d5 * d5 + d7 * d7 + d9 * d9);
                    if (d13 != 0.0D) {
                        d5 = d5 / d13;
                        d7 = d7 / d13;
                        d9 = d9 / d13;
                        double d14 = Explosion.getBlockDensity(vec3d, entity);
                        double d10 = (1.0D - d12) * d14;

                        CraftEventFactory.entityDamage = this.exploder;
                        ((EntityBridge) entity).bridge$setForceExplosionKnockback(false);
                        boolean wasDamaged = entity.attackEntityFrom(this.getDamageSource(), (float) ((int) ((d10 * d10 + d10) / 2.0D * 7.0D * (double) f3 + 1.0D)));
                        CraftEventFactory.entityDamage = null;
                        if (!wasDamaged && !(entity instanceof TNTEntity || entity instanceof FallingBlockEntity) && !((EntityBridge) entity).bridge$isForceExplosionKnockback()) {
                            continue;
                        }

                        double d11 = d10;
                        if (entity instanceof LivingEntity) {
                            d11 = ProtectionEnchantment.getBlastDamageReduction((LivingEntity) entity, d10);
                        }

                        entity.setMotion(entity.getMotion().add(d5 * d11, d7 * d11, d9 * d11));
                        if (entity instanceof PlayerEntity) {
                            PlayerEntity playerentity = (PlayerEntity) entity;
                            if (!playerentity.isSpectator() && (!playerentity.isCreative() || !playerentity.abilities.isFlying)) {
                                this.playerKnockbackMap.put(playerentity, new Vector3d(d5 * d10, d7 * d10, d9 * d10));
                            }
                        }
                    }
                }
            }
        }

    }

    public boolean wasCanceled = false;

    @Override
    public boolean bridge$wasCancelled() {
        return wasCanceled;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void doExplosionB(boolean spawnParticles) {
        if (this.world.isRemote) {
            this.world.playSound(this.x, this.y, this.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F, (1.0F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.2F) * 0.7F, false);
        }

        boolean flag = this.mode != Explosion.Mode.NONE;
        if (spawnParticles) {
            if (!(this.size < 2.0F) && flag) {
                this.world.addParticle(ParticleTypes.EXPLOSION_EMITTER, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
            } else {
                this.world.addParticle(ParticleTypes.EXPLOSION, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
            }
        }

        if (flag) {
            ObjectArrayList<Pair<ItemStack, BlockPos>> objectarraylist = new ObjectArrayList<>();
            Collections.shuffle(this.affectedBlockPositions, this.world.rand);

            float yield = this.callBlockExplodeEvent();

            if (Float.isNaN(yield)) {
                this.wasCanceled = true;
                return;
            }

            for (BlockPos blockpos : this.affectedBlockPositions) {
                BlockState blockstate = this.world.getBlockState(blockpos);
                Block block = blockstate.getBlock();
                if (!blockstate.isAir(this.world, blockpos)) {
                    BlockPos blockpos1 = blockpos.toImmutable();
                    this.world.getProfiler().startSection("explosion_blocks");
                    if (blockstate.canDropFromExplosion(this.world, blockpos, (Explosion) (Object) this) && this.world instanceof ServerWorld) {
                        TileEntity tileentity = blockstate.hasTileEntity() ? this.world.getTileEntity(blockpos) : null;
                        LootContext.Builder lootcontext$builder = (new LootContext.Builder((ServerWorld)this.world)).withRandom(this.world.rand).withParameter(LootParameters.field_237457_g_, Vector3d.copyCentered(blockpos)).withParameter(LootParameters.TOOL, ItemStack.EMPTY).withNullableParameter(LootParameters.BLOCK_ENTITY, tileentity).withNullableParameter(LootParameters.THIS_ENTITY, this.exploder);
                        if (this.mode == Explosion.Mode.DESTROY || yield < 1.0F) {
                            lootcontext$builder.withParameter(LootParameters.EXPLOSION_RADIUS, 1.0F / yield);
                        }

                        blockstate.getDrops(lootcontext$builder).forEach((stack) -> {
                            handleExplosionDrops(objectarraylist, stack, blockpos1);
                        });
                    }

                    blockstate.onBlockExploded(this.world, blockpos, (Explosion) (Object) this);
                    this.world.getProfiler().endSection();
                }
            }

            for (Pair<ItemStack, BlockPos> pair : objectarraylist) {
                Block.spawnAsEntity(this.world, pair.getSecond(), pair.getFirst());
            }
        }

        if (this.causesFire) {
            for (BlockPos blockpos2 : this.affectedBlockPositions) {
                if (this.random.nextInt(3) == 0 && this.world.getBlockState(blockpos2).isAir() && this.world.getBlockState(blockpos2.down()).isOpaqueCube(this.world, blockpos2.down())) {
                    BlockIgniteEvent event = CraftEventFactory.callBlockIgniteEvent(this.world, blockpos2.getX(), blockpos2.getY(), blockpos2.getZ(), (Explosion) (Object) this);
                    if (!event.isCancelled()) {
                        this.world.setBlockState(blockpos2, AbstractFireBlock.getFireForPlacement(this.world, blockpos2));
                    }
                }
            }
        }
    }

    @Inject(method = "handleExplosionDrops", cancellable = true, at = @At("HEAD"))
    private static void arclight$fix(ObjectArrayList<Pair<ItemStack, BlockPos>> dropPositionArray, ItemStack stack, BlockPos pos, CallbackInfo ci) {
        if (stack.isEmpty()) ci.cancel();
    }

    private float callBlockExplodeEvent() {
        org.bukkit.World world = ((WorldBridge) this.world).bridge$getWorld();
        org.bukkit.entity.Entity exploder = this.exploder == null ? null : ((EntityBridge) this.exploder).bridge$getBukkitEntity();
        Location location = new Location(world, this.x, this.y, this.z);
        List<org.bukkit.block.Block> blockList = Lists.newArrayList();
        for (int i = this.affectedBlockPositions.size() - 1; i >= 0; i--) {
            BlockPos blockPos = this.affectedBlockPositions.get(i);
            org.bukkit.block.Block block = world.getBlockAt(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            if (!block.getType().isAir()) {
                blockList.add(block);
            }
        }

        boolean cancelled;
        List<org.bukkit.block.Block> bukkitBlocks;
        float yield;

        if (exploder != null) {
            EntityExplodeEvent event = new EntityExplodeEvent(exploder, location, blockList, this.mode == Explosion.Mode.DESTROY ? 1.0F / this.size : 1.0F);
            Bukkit.getPluginManager().callEvent(event);
            cancelled = event.isCancelled();
            bukkitBlocks = event.blockList();
            yield = event.getYield();
        } else {
            BlockExplodeEvent event = new BlockExplodeEvent(location.getBlock(), blockList, this.mode == Explosion.Mode.DESTROY ? 1.0F / this.size : 1.0F);
            Bukkit.getPluginManager().callEvent(event);
            cancelled = event.isCancelled();
            bukkitBlocks = event.blockList();
            yield = event.getYield();
        }

        this.affectedBlockPositions.clear();

        for (org.bukkit.block.Block block : bukkitBlocks) {
            BlockPos blockPos = new BlockPos(block.getX(), block.getY(), block.getZ());
            this.affectedBlockPositions.add(blockPos);
        }
        return cancelled ? Float.NaN : yield;
    }
}
