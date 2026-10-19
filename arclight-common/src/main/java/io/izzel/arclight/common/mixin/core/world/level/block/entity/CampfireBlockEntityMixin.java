package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeHolderBridge;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.block.CampfireStartEvent;
import org.bukkit.inventory.CampfireRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Optional;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin extends BlockEntityMixin {

    // @formatter:off
    @Shadow @Final public int[] cookingTime;
    // @formatter:on

    @Decorate(method = "cookTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Containers;dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"))
    private static void arclight$cookEvent(Level instance, double x, double y, double z, ItemStack resultStack,
                                           ServerLevel level, BlockPos blockPos, BlockState blockState, CampfireBlockEntity campfireBlockEntity,
                                           @Local(ordinal = 0) ItemStack sourceStack) throws Throwable {
        CraftItemStack source = CraftItemStack.asCraftMirror(sourceStack);
        org.bukkit.inventory.ItemStack result = CraftItemStack.asBukkitCopy(resultStack);

        BlockCookEvent blockCookEvent = new BlockCookEvent(CraftBlock.at(level, blockPos), source, result);
        Bukkit.getPluginManager().callEvent(blockCookEvent);

        if (blockCookEvent.isCancelled()) {
            DecorationOps.cancel().invoke();
            return;
        }

        result = blockCookEvent.getResult();
        resultStack = CraftItemStack.asNMSCopy(result);
        DecorationOps.callsite().invoke(instance, x, y, z, resultStack);
    }

    @Inject(method = "placeFood", locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;cookingTime:[I", opcode = org.objectweb.asm.Opcodes.GETFIELD),
        require = 1, expect = 1, allow = 1)
    private void arclight$validateStartRecipe(ServerLevel serverLevel, LivingEntity entity, ItemStack stack,
            CallbackInfoReturnable<Boolean> cir, int i, ItemStack oldStack, Optional<RecipeHolder<CampfireCookingRecipe>> recipe,
            @Share("arclight$startRecipe") LocalRef<CampfireRecipe> prepared) {
        var converted = ((RecipeHolderBridge) (Object) recipe.orElseThrow()).bridge$toBukkitRecipe();
        if (!(converted instanceof CampfireRecipe campfire)) {
            throw new IllegalArgumentException("Campfire recipe cannot provide a valid Bukkit cooking result: " + recipe.get().id());
        }
        prepared.set(campfire);
    }

    @Inject(method = "placeFood", locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;cookingProgress:[I"))
    private void arclight$cookStart(ServerLevel serverLevel, LivingEntity entity, ItemStack stack, CallbackInfoReturnable<Boolean> cir, int i, ItemStack oldStack, Optional<RecipeHolder<CampfireCookingRecipe>> recipe,
            @Share("arclight$startRecipe") LocalRef<CampfireRecipe> prepared) {
        var event = new CampfireStartEvent(CraftBlock.at(this.level, this.worldPosition), CraftItemStack.asCraftMirror(stack), prepared.get());
        Bukkit.getPluginManager().callEvent(event);
        this.cookingTime[i] = event.getTotalCookTime();
    }
}
