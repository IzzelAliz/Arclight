package io.izzel.arclight.impl.mixin.v1_14.world;

import io.izzel.arclight.common.bridge.world.ExplosionBridge;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootParameters;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.BlockIgniteEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Random;

@Mixin(Explosion.class)
public abstract class ExplosionMixin_1_14 implements ExplosionBridge {

    // @formatter:off
    @Shadow @Final private World world;
    @Shadow @Final private Explosion.Mode mode;
    @Shadow @Mutable @Final private float size;
    @Shadow @Final private List<BlockPos> affectedBlockPositions;
    @Shadow @Final private double x;
    @Shadow @Final private double y;
    @Shadow @Final private double z;
    @Shadow @Final private boolean causesFire;
    @Shadow @Final private Random random;
    @Accessor("exploder") public abstract Entity bridge$getExploder();
    @Accessor("size") public abstract float bridge$getSize();
    @Accessor("size") public abstract void bridge$setSize(float size);
    @Accessor("mode") public abstract Explosion.Mode bridge$getMode();
    // @formatter:on

    public boolean wasCanceled = false;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void doExplosionB(boolean spawnParticles) {
        if (world.isRemote) {
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

            float yield = this.bridge$callBlockExplodeEvent();

            if (Float.isNaN(yield)) {
                this.wasCanceled = true;
                return;
            }

            for (BlockPos blockpos : this.affectedBlockPositions) {
                BlockState blockstate = this.world.getBlockState(blockpos);
                Block block = blockstate.getBlock();
                if (spawnParticles) {
                    double d0 = ((float) blockpos.getX() + this.world.rand.nextFloat());
                    double d1 = ((float) blockpos.getY() + this.world.rand.nextFloat());
                    double d2 = ((float) blockpos.getZ() + this.world.rand.nextFloat());
                    double d3 = d0 - this.x;
                    double d4 = d1 - this.y;
                    double d5 = d2 - this.z;
                    double d6 = MathHelper.sqrt(d3 * d3 + d4 * d4 + d5 * d5);
                    d3 = d3 / d6;
                    d4 = d4 / d6;
                    d5 = d5 / d6;
                    double d7 = 0.5D / (d6 / (double) this.size + 0.1D);
                    d7 = d7 * (double) (this.world.rand.nextFloat() * this.world.rand.nextFloat() + 0.3F);
                    d3 = d3 * d7;
                    d4 = d4 * d7;
                    d5 = d5 * d7;
                    this.world.addParticle(ParticleTypes.POOF, (d0 + this.x) / 2.0D, (d1 + this.y) / 2.0D, (d2 + this.z) / 2.0D, d3, d4, d5);
                    this.world.addParticle(ParticleTypes.SMOKE, d0, d1, d2, d3, d4, d5);
                }

                if (!blockstate.isAir(this.world, blockpos)) {
                    if (this.world instanceof ServerWorld && blockstate.canDropFromExplosion(this.world, blockpos, (Explosion) (Object) this)) {
                        TileEntity tileentity = blockstate.hasTileEntity() ? this.world.getTileEntity(blockpos) : null;
                        LootContext.Builder lootcontext$builder = (new LootContext.Builder((ServerWorld) this.world)).withRandom(this.world.rand).withParameter(LootParameters.POSITION, blockpos).withParameter(LootParameters.TOOL, ItemStack.EMPTY).withNullableParameter(LootParameters.BLOCK_ENTITY, tileentity);
                        if (this.mode == Explosion.Mode.DESTROY || yield < 1.0F) {
                            lootcontext$builder.withParameter(LootParameters.EXPLOSION_RADIUS, 1.0F / yield);
                        }

                        Block.spawnDrops(blockstate, lootcontext$builder);
                    }

                    blockstate.onBlockExploded(this.world, blockpos, (Explosion) (Object) this);
                }
            }
        }

        if (this.causesFire) {
            for (BlockPos blockPos : this.affectedBlockPositions) {
                if (this.world.getBlockState(blockPos).isAir(world, blockPos) && this.world.getBlockState(blockPos.down()).isOpaqueCube(this.world, blockPos.down()) && this.random.nextInt(3) == 0) {
                    BlockIgniteEvent event = CraftEventFactory.callBlockIgniteEvent(this.world, blockPos.getX(), blockPos.getY(), blockPos.getZ(), (Explosion) (Object) this);
                    if (!event.isCancelled()) {
                        this.world.setBlockState(blockPos, Blocks.FIRE.getDefaultState());
                    }
                }
            }
        }
    }

    @Override
    public boolean bridge$wasCancelled() {
        return wasCanceled;
    }
}
