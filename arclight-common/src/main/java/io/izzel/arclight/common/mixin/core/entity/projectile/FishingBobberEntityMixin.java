package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootParameterSets;
import net.minecraft.world.storage.loot.LootParameters;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTables;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.FishHook;
import org.bukkit.event.player.PlayerFishEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collections;
import java.util.List;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin extends EntityMixin {

    // @formatter:off
    @Shadow public PlayerEntity angler;
    @Shadow public Entity caughtEntity;
    @Shadow protected abstract void bringInHookedEntity();
    @Shadow private int ticksCatchable;
    @Shadow @Final private int luck;
    @Shadow private boolean inGround;
    // @formatter:on

    @Inject(method = "checkCollision", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/util/math/RayTraceResult;getType()Lnet/minecraft/util/math/RayTraceResult$Type;"))
    private void arclight$entityHit(CallbackInfo ci, RayTraceResult result) {
        CraftEventFactory.callProjectileHitEvent((FishingBobberEntity) (Object) this, result);
    }

    @Inject(method = "catchingFish", at = @At(value = "FIELD", shift = At.Shift.AFTER, ordinal = 0, target = "Lnet/minecraft/entity/projectile/FishingBobberEntity;ticksCatchableDelay:I"))
    private void arclight$attemptFail(BlockPos blockPos, CallbackInfo ci) {
        PlayerFishEvent event = new PlayerFishEvent(((ServerPlayerEntityBridge) this.angler).bridge$getBukkitEntity(), null, (FishHook) this.getBukkitEntity(), PlayerFishEvent.State.FAILED_ATTEMPT);
        Bukkit.getPluginManager().callEvent(event);
    }

    @Inject(method = "catchingFish", cancellable = true, at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/entity/projectile/FishingBobberEntity;getMotion()Lnet/minecraft/util/math/Vec3d;"))
    private void arclight$fishBite(BlockPos blockPos, CallbackInfo ci) {
        PlayerFishEvent event = new PlayerFishEvent(((ServerPlayerEntityBridge) this.angler).bridge$getBukkitEntity(), null, (FishHook) this.getBukkitEntity(), PlayerFishEvent.State.BITE);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public int handleHookRetraction(ItemStack p_146034_1_) {
        if (!this.world.isRemote && this.angler != null) {
            int i = 0;
            ItemFishedEvent event = null;
            if (this.caughtEntity != null) {
                PlayerFishEvent fishEvent = new PlayerFishEvent(((ServerPlayerEntityBridge) this.angler).bridge$getBukkitEntity(), ((EntityBridge) this.caughtEntity).bridge$getBukkitEntity(), (FishHook) this.getBukkitEntity(), PlayerFishEvent.State.CAUGHT_ENTITY);
                Bukkit.getPluginManager().callEvent(fishEvent);
                if (fishEvent.isCancelled()) {
                    return 0;
                }
                this.bringInHookedEntity();
                CriteriaTriggers.FISHING_ROD_HOOKED.trigger((ServerPlayerEntity) this.angler, p_146034_1_, (FishingBobberEntity) (Object) this, Collections.emptyList());
                this.world.setEntityState((FishingBobberEntity) (Object) this, (byte) 31);
                i = this.caughtEntity instanceof ItemEntity ? 3 : 5;
            } else if (this.ticksCatchable > 0) {
                LootContext.Builder lootcontext$builder = (new LootContext.Builder((ServerWorld) this.world)).withParameter(LootParameters.POSITION, new BlockPos((FishingBobberEntity) (Object) this)).withParameter(LootParameters.TOOL, p_146034_1_).withRandom(this.rand).withLuck((float) this.luck + this.angler.getLuck());
                lootcontext$builder.withParameter(LootParameters.KILLER_ENTITY, this.angler).withParameter(LootParameters.THIS_ENTITY, (FishingBobberEntity) (Object) this);
                LootTable loottable = this.world.getServer().getLootTableManager().getLootTableFromLocation(LootTables.GAMEPLAY_FISHING);
                List<ItemStack> list = loottable.generate(lootcontext$builder.build(LootParameterSets.FISHING));
                event = new ItemFishedEvent(list, this.inGround ? 2 : 1, (FishingBobberEntity) (Object) this);
                MinecraftForge.EVENT_BUS.post(event);
                if (event.isCanceled()) {
                    this.remove();
                    return event.getRodDamage();
                }
                CriteriaTriggers.FISHING_ROD_HOOKED.trigger((ServerPlayerEntity) this.angler, p_146034_1_, (FishingBobberEntity) (Object) this, list);

                for (ItemStack itemstack : list) {
                    ItemEntity itementity = new ItemEntity(this.world, this.posX, this.posY, this.posZ, itemstack);
                    PlayerFishEvent playerFishEvent = new PlayerFishEvent(((ServerPlayerEntityBridge) this.angler).bridge$getBukkitEntity(), ((EntityBridge) itementity).bridge$getBukkitEntity(), (FishHook) this.getBukkitEntity(), PlayerFishEvent.State.CAUGHT_FISH);
                    playerFishEvent.setExpToDrop(this.rand.nextInt(6) + 1);
                    Bukkit.getPluginManager().callEvent(playerFishEvent);

                    if (playerFishEvent.isCancelled()) {
                        return 0;
                    }

                    double d0 = this.angler.posX - this.posX;
                    double d1 = this.angler.posY - this.posY;
                    double d2 = this.angler.posZ - this.posZ;
                    double d3 = 0.1D;
                    itementity.setMotion(d0 * 0.1D, d1 * 0.1D + Math.sqrt(Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2)) * 0.08D, d2 * 0.1D);
                    this.world.addEntity(itementity);
                    if (playerFishEvent.getExpToDrop() > 0) {
                        this.angler.world.addEntity(new ExperienceOrbEntity(this.angler.world, this.angler.posX, this.angler.posY + 0.5D, this.angler.posZ + 0.5D, playerFishEvent.getExpToDrop()));
                    }
                    if (itemstack.getItem().isIn(ItemTags.FISHES)) {
                        this.angler.addStat(Stats.FISH_CAUGHT, 1);
                    }
                }

                i = 1;
            }

            if (this.inGround) {
                PlayerFishEvent playerFishEvent = new PlayerFishEvent(((ServerPlayerEntityBridge) this.angler).bridge$getBukkitEntity(), null, (FishHook) this.getBukkitEntity(), PlayerFishEvent.State.IN_GROUND);
                Bukkit.getPluginManager().callEvent(playerFishEvent);

                if (playerFishEvent.isCancelled()) {
                    return 0;
                }
                i = 2;
            }

            if (i == 0) {
                PlayerFishEvent playerFishEvent = new PlayerFishEvent(((ServerPlayerEntityBridge) this.angler).bridge$getBukkitEntity(), null, (FishHook) this.getBukkitEntity(), PlayerFishEvent.State.REEL_IN);
                Bukkit.getPluginManager().callEvent(playerFishEvent);
                if (playerFishEvent.isCancelled()) {
                    return 0;
                }
            }

            this.remove();
            return event == null ? i : event.getRodDamage();
        } else {
            return 0;
        }
    }
}
