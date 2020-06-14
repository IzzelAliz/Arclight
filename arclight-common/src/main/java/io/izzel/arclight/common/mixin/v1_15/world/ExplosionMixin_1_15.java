package io.izzel.arclight.common.mixin.v1_15.world;

import com.mojang.datafixers.util.Pair;
import io.izzel.arclight.common.bridge.world.ExplosionBridge;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import java.util.Random;

@Mixin(Explosion.class)
public abstract class ExplosionMixin_1_15 implements ExplosionBridge {

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
    @Shadow @Final public Entity exploder;
    @Shadow private static void func_229976_a_(ObjectArrayList<Pair<ItemStack, BlockPos>> p_229976_0_, ItemStack p_229976_1_, BlockPos p_229976_2_) { }
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

            float yield = this.bridge$callBlockExplodeEvent();

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
                        LootContext.Builder lootcontext$builder = (new LootContext.Builder((ServerWorld) this.world)).withRandom(this.world.rand).withParameter(LootParameters.POSITION, blockpos).withParameter(LootParameters.TOOL, ItemStack.EMPTY).withNullableParameter(LootParameters.BLOCK_ENTITY, tileentity).withNullableParameter(LootParameters.THIS_ENTITY, this.exploder);
                        if (this.mode == Explosion.Mode.DESTROY || yield < 1.0F) {
                            lootcontext$builder.withParameter(LootParameters.EXPLOSION_RADIUS, 1.0F / yield);
                        }

                        blockstate.getDrops(lootcontext$builder).forEach((p_229977_2_) -> {
                            func_229976_a_(objectarraylist, p_229977_2_, blockpos1);
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
                        this.world.setBlockState(blockpos2, Blocks.FIRE.getDefaultState());
                    }
                }
            }
        }
    }

    @Inject(method = "func_229976_a_", cancellable = true, at = @At("HEAD"))
    private static void arclight$fix(ObjectArrayList<Pair<ItemStack, BlockPos>> p_229976_0_, ItemStack stack, BlockPos p_229976_2_, CallbackInfo ci) {
        if (stack.isEmpty()) ci.cancel();
    }

    @Override
    public boolean bridge$wasCancelled() {
        return wasCanceled;
    }
}
