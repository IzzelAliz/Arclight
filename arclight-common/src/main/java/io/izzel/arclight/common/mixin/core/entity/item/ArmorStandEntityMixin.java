package io.izzel.arclight.common.mixin.core.entity.item;

import com.google.common.collect.Lists;
import io.izzel.arclight.common.mixin.core.entity.LivingEntityMixin;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(ArmorStandEntity.class)
public abstract class ArmorStandEntityMixin extends LivingEntityMixin {

    @Shadow private boolean canInteract;

    @Override
    public float getBukkitYaw() {
        return this.rotationYaw;
    }

    @Inject(method = "attackEntityFrom", cancellable = true, at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/entity/item/ArmorStandEntity;remove()V"))
    public void arclight$damageDropOut(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((ArmorStandEntity) (Object) this, source, amount)) {
            cir.setReturnValue(false);
        } else {
            arclight$callEntityDeath();
        }
    }

    @Inject(method = "attackEntityFrom", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/DamageSource;isExplosion()Z"))
    public void arclight$damageNormal(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((ArmorStandEntity) (Object) this, source, amount, true, this.canInteract)) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "attackEntityFrom", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/item/ArmorStandEntity;canInteract:Z"))
    private boolean arclight$softenCondition(ArmorStandEntity entity) {
        return true;
    }

    @Inject(method = "attackEntityFrom", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/entity/item/ArmorStandEntity;remove()V"))
    private void arclight$damageDeath1(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        arclight$callEntityDeath();
    }

    @Inject(method = "attackEntityFrom", at = @At(value = "INVOKE", ordinal = 2, target = "Lnet/minecraft/entity/item/ArmorStandEntity;remove()V"))
    private void arclight$damageDeath2(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        arclight$callEntityDeath();
    }

    @Inject(method = "func_213817_e", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/ArmorStandEntity;remove()V"))
    private void arclight$deathEvent2(DamageSource p_213817_1_, float p_213817_2_, CallbackInfo ci) {
        arclight$callEntityDeath();
    }

    @Redirect(method = "func_213815_f", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;spawnAsEntity(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/item/ItemStack;)V"))
    private void arclight$captureDrops1(World worldIn, BlockPos pos, ItemStack stack) {
        arclight$tryCaptureDrops(worldIn, pos, stack);
    }

    @Redirect(method = "func_213816_g", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/ArmorStandEntity;spawnDrops(Lnet/minecraft/util/DamageSource;)V"))
    private void arclight$dropLater(ArmorStandEntity entity, DamageSource damageSourceIn) {
    }

    @Redirect(method = "func_213816_g", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;spawnAsEntity(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/item/ItemStack;)V"))
    private void arclight$captureDropsDeath(World worldIn, BlockPos pos, ItemStack stack) {
        arclight$tryCaptureDrops(worldIn, pos, stack);
    }

    @Inject(method = "func_213816_g", at = @At("RETURN"))
    private void arclight$spawnLast(DamageSource source, CallbackInfo ci) {
        this.spawnDrops(source);
    }

    @Override
    protected boolean canDropLoot() {
        return true;
    }

    @Inject(method = "onKillCommand", at = @At("HEAD"))
    private void arclight$deathEvent(CallbackInfo ci) {
        arclight$callEntityDeath();
    }

    private void arclight$tryCaptureDrops(World worldIn, BlockPos pos, ItemStack stack) {
        if (!worldIn.isRemote && !stack.isEmpty() && worldIn.getGameRules().getBoolean(GameRules.DO_TILE_DROPS) && !worldIn.restoringBlockSnapshots) { // do not drop items while restoring blockstates, prevents item dupe
            ItemEntity itementity = new ItemEntity(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack);
            arclight$drops().add(itementity);
        }
    }

    private Collection<ItemEntity> arclight$drops() {
        Collection<ItemEntity> drops = this.captureDrops();
        return drops == null ? this.captureDrops(new ArrayList<>()) : drops;
    }

    private void arclight$callEntityDeath() {
        Collection<ItemEntity> captureDrops = this.captureDrops(null);
        List<org.bukkit.inventory.ItemStack> drops;
        if (captureDrops == null) {
            drops = new ArrayList<>();
        } else if (captureDrops instanceof List) {
            drops = Lists.transform((List<ItemEntity>) captureDrops, e -> CraftItemStack.asCraftMirror(e.getItem()));
        } else {
            drops = captureDrops.stream().map(ItemEntity::getItem).map(CraftItemStack::asCraftMirror).collect(Collectors.toList());
        }
        CraftEventFactory.callEntityDeathEvent((ArmorStandEntity) (Object) this, drops);
    }
}
