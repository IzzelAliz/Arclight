package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.level.block.entity.AbstractFurnaceBlockEntityBridge;
import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeHolderBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.inventory.CraftItemType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin extends BaseContainerBlockEntityMixin implements AbstractFurnaceBlockEntityBridge {

    // @formatter:off
    @Shadow protected NonNullList<ItemStack> items;
    @Shadow protected abstract int getBurnDuration(FuelValues fuelValues, ItemStack stack);
    @Shadow protected abstract boolean isLit();
    @Shadow int litTimeRemaining;
    @Shadow int cookingTimer;
    @Shadow int cookingTotalTime;
    @Shadow @Final private Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed;
    @Shadow public abstract List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel p_154996_, Vec3 p_154997_);
    // @formatter:on

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = MAX_STACK;

    // Static decorator used to be allowed to inline into non-static context,
    // but the result will be incorrect.
    // This will cause the injected method to become invalid.
    // See PSI
    // @Decorate(method = "burn", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/core/NonNullList;get(I)Ljava/lang/Object;"))
    // private static <E> E arclight$furnaceSmelt(NonNullList<E> instance, int i, @Local(ordinal = 0) AbstractFurnaceBlockEntity blockEntity, @Local(ordinal = -1) ItemStack itemStack2, @Local(ordinal = -2) ItemStack itemStack1) throws Throwable

    @Decorate(method = "serverTick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;isLit()Z"),
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;litTotalTime:I")))
    private static boolean arclight$setBurnTime(AbstractFurnaceBlockEntity furnace) throws Throwable {
        ItemStack itemStack = furnace.getItem(1);
        CraftItemStack fuel = CraftItemStack.asCraftMirror(itemStack);
        AbstractFurnaceBlockEntityMixin furnaceMixin = (AbstractFurnaceBlockEntityMixin) (Object) furnace;
        FurnaceBurnEvent furnaceBurnEvent = new FurnaceBurnEvent(CraftBlock.at(furnace.level, furnace.getBlockPos()), fuel, furnaceMixin.litTimeRemaining);
        Bukkit.getPluginManager().callEvent(furnaceBurnEvent);
        if (furnaceBurnEvent.isCancelled()) {
            return (boolean) DecorationOps.cancel().invoke();
        }
        furnaceMixin.litTimeRemaining = furnaceBurnEvent.getBurnTime();
        return (boolean) DecorationOps.callsite().invoke(furnace) && furnaceBurnEvent.isBurning();
    }

    @Decorate(method = "serverTick", inject = true, at = @At(value = "FIELD", ordinal = 0, target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;cookingTimer:I"))
    private static void arclight$startSmelt(ServerLevel level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity furnace,
                                            @Local(ordinal = -1) RecipeHolder<?> recipe) {
        AbstractFurnaceBlockEntityMixin furnaceMixin = (AbstractFurnaceBlockEntityMixin) (Object) furnace;
        if (recipe != null && furnaceMixin.cookingTimer == 0) {
            CraftItemStack source = CraftItemStack.asCraftMirror(furnace.getItem(0));
            if (((RecipeHolderBridge) (Object) recipe).bridge$toBukkitRecipe() instanceof CookingRecipe<?> cookingRecipe) {
                FurnaceStartSmeltEvent event = new FurnaceStartSmeltEvent(CraftBlock.at(level, pos), source, cookingRecipe);
                Bukkit.getPluginManager().callEvent(event);
                furnaceMixin.cookingTotalTime = event.getTotalCookTime();
            }
        }
    }

    private static AbstractFurnaceBlockEntity arclight$captureFurnace;
    private static Player arclight$capturePlayer;
    private static ItemStack arclight$item;
    private static int arclight$captureAmount;

    public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel world, Vec3 vec, BlockPos pos, Player entity, ItemStack itemStack, int amount) {
        try {
            arclight$item = itemStack;
            arclight$captureAmount = amount;
            arclight$captureFurnace = (AbstractFurnaceBlockEntity) (Object) this;
            arclight$capturePlayer = entity;
            List<RecipeHolder<?>> list = this.getRecipesToAwardAndPopExperience(world, vec);
            entity.awardRecipes(list);
            this.recipesUsed.clear();
            return list;
        } finally {
            arclight$item = null;
            arclight$captureAmount = 0;
            arclight$captureFurnace = null;
            arclight$capturePlayer = null;
        }
    }

    @Override
    public List<RecipeHolder<?>> bridge$dropExp(ServerPlayer entity, ItemStack itemStack, int amount) {
        return getRecipesToAwardAndPopExperience(entity.level(), entity.position(), this.worldPosition, entity, itemStack, amount);
    }

    @Redirect(method = "createExperience", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"))
    private static void arclight$expEvent(ServerLevel level, Vec3 vec3, int amount) {
        if (arclight$capturePlayer != null && arclight$captureAmount != 0) {
            FurnaceExtractEvent event = new FurnaceExtractEvent(((ServerPlayerBridge) arclight$capturePlayer).bridge$getBukkitEntity(),
                CraftBlock.at(level, arclight$captureFurnace.getBlockPos()), CraftItemType.minecraftToBukkit(arclight$item.getItem()), arclight$captureAmount, amount);
            Bukkit.getPluginManager().callEvent(event);
            amount = event.getExpToDrop();
        }
        ExperienceOrb.award(level, vec3, amount);
    }

    @Override
    public List<ItemStack> getContents() {
        return this.items;
    }

    @Override
    public void onOpen(CraftHumanEntity who) {
        transaction.add(who);
    }

    @Override
    public void onClose(CraftHumanEntity who) {
        transaction.remove(who);
    }

    @Override
    public List<HumanEntity> getViewers() {
        return transaction;
    }

    @Override
    public void setOwner(InventoryHolder owner) {
    }

    @Override
    public int getMaxStackSize() {
        if (maxStack == 0) maxStack = MAX_STACK;
        return maxStack;
    }

    @Override
    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }

    @Override
    public int bridge$getBurnDuration(ItemStack stack) {
        if (this.level instanceof ServerLevel serverLevel) {
            return this.getBurnDuration(serverLevel.fuelValues(), stack);
        }
        return 0;
    }

    @Override
    public boolean bridge$isLit() {
        return this.isLit();
    }

    public Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> getRecipesUsed() {
        return this.recipesUsed;
    }
}
