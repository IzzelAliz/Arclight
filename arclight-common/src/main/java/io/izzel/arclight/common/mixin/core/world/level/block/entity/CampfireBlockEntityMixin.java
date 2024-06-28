package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeHolderBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
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
    @Shadow public abstract Optional<RecipeHolder<CampfireCookingRecipe>> getCookableRecipe(ItemStack p_59052_);
    @Shadow @Final public int[] cookingTime;
    // @formatter:on

    @Decorate(method = "cookTick", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Containers;dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"))
    private static void arclight$cookEvent(Level level, BlockPos blockPos, BlockState blockState, CampfireBlockEntity campfireBlockEntity,
                                           @Local(ordinal = 0) ItemStack sourceStack, @Local(ordinal = -1) ItemStack resultStack) throws Throwable {
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
        DecorationOps.blackhole().invoke(resultStack);
    }

    @Inject(method = "placeFood", locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;cookingProgress:[I"))
    private void arclight$cookStart(LivingEntity p_238285_, ItemStack stack, int p_238287_, CallbackInfoReturnable<Boolean> cir, int i) {
        var event = new CampfireStartEvent(CraftBlock.at(this.level, this.worldPosition), CraftItemStack.asCraftMirror(stack), (CampfireRecipe) ((RecipeHolderBridge) (Object) getCookableRecipe(stack).get()).bridge$toBukkitRecipe());
        Bukkit.getPluginManager().callEvent(event);
        this.cookingTime[i] = event.getTotalCookTime();
    }
}
