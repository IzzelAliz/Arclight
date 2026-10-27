package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.InventoryHolder;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin extends BaseContainerBlockEntityMixin implements AbstractFurnaceBlockEntityBridge {

    // @formatter:off
    @Shadow protected NonNullList<ItemStack> items;
    @Shadow protected abstract int getBurnDuration(FuelValues fuelValues, ItemStack stack);
    @Shadow int litTimeRemaining;
    @Shadow int cookingTimer;
    @Shadow int cookingTotalTime;
    @Shadow @Final private Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed;
    @Shadow public abstract List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel p_154996_, Vec3 p_154997_);
    // @formatter:on

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = MAX_STACK;
    private net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> arclight$recipeType;

    @org.spongepowered.asm.mixin.injection.Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$captureRecipeType(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state,
            net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> recipeType,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        this.arclight$recipeType = recipeType;
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> bridge$getRecipeType() {
        return this.arclight$recipeType;
    }

    @Decorate(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getBurnDuration(Lnet/minecraft/world/level/block/entity/FuelValues;Lnet/minecraft/world/item/ItemStack;)I"))
    private static int arclight$setBurnTime(AbstractFurnaceBlockEntity furnace, FuelValues fuelValues, ItemStack fuel,
                                           @Local(allocate = "arclightFurnaceBurn") FurnaceBurnEvent event) throws Throwable {
        int duration = (int) DecorationOps.callsite().invoke(furnace, fuelValues, fuel);
        event = new FurnaceBurnEvent(CraftBlock.at(furnace.getLevel(), furnace.getBlockPos()), CraftItemStack.asCraftMirror(fuel), duration);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            // Decorate replaces this typed invoke/IRETURN with serverTick's RETURN.
            return (int) DecorationOps.cancel().invoke();
        }
        DecorationOps.blackhole().invoke(event);
        return event.getBurnTime();
    }

    // Both timer fields retain the event duration, but the native newLitTime branch
    // may consume fuel and start cooking only if the event also says it is burning.
    @Decorate(method = "serverTick", inject = true, at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
        target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;litTotalTime:I", shift = At.Shift.AFTER))
    private static void arclight$applyBurningState(@Local(ordinal = -1) int newLitTime,
                                            @Local(allocate = "arclightFurnaceBurn") FurnaceBurnEvent event) throws Throwable {
        if (!event.isBurning()) {
            newLitTime = 0;
            DecorationOps.blackhole().invoke(newLitTime);
        }
    }

    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;setRecipeUsed(Lnet/minecraft/world/item/crafting/RecipeHolder;)V"))
    private static void arclight$creditSmelt(AbstractFurnaceBlockEntity furnace, RecipeHolder<?> recipe, Operation<Void> original,
                                            @Share(value = "smelted", namespace = "arclight:furnace") LocalBooleanRef smelted) {
        if (smelted.get()) {
            original.call(furnace, recipe);
        }
    }

    @Decorate(method = "serverTick", inject = true, at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, ordinal = 0, target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;cookingTimer:I"))
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

    // Exact fixed-CraftBukkit collection ABI. This overload collects only; recipe
    // awards, crafted criteria and clearing belong to the award/drop entry point.
    public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel world, Vec3 vec, BlockPos pos,
                                                                  ServerPlayer entity, ItemStack itemStack, int amount) {
        var previous = ArclightCaptures.furnaceExtraction;
        ArclightCaptures.furnaceExtraction = com.mojang.datafixers.util.Pair.of(pos, org.apache.commons.lang3.tuple.Triple.of(entity, itemStack, amount));
        try {
            return this.getRecipesToAwardAndPopExperience(world, vec);
        } finally {
            ArclightCaptures.furnaceExtraction = previous;
        }
    }

    @WrapMethod(method = "getRecipesToAwardAndPopExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;)Ljava/util/List;")
    private List<RecipeHolder<?>> arclight$experienceScope(ServerLevel world, Vec3 vec, Operation<List<RecipeHolder<?>>> original) {
        var previous = ArclightCaptures.furnaceExperience;
        var request = ArclightCaptures.furnaceExtraction;
        ArclightCaptures.furnaceExtraction = null;
        ArclightCaptures.furnaceExperience = request != null ? request : com.mojang.datafixers.util.Pair.of(this.worldPosition,
            org.apache.commons.lang3.tuple.Triple.of(null, null, 0));
        try {
            return original.call(world, vec);
        } finally {
            ArclightCaptures.furnaceExperience = previous;
        }
    }

    public void awardUsedRecipesAndPopExperience(ServerPlayer entity, ItemStack itemStack, int amount) {
        bridge$dropExp(entity, itemStack, amount);
    }

    @Override
    public List<RecipeHolder<?>> bridge$dropExp(ServerPlayer entity, ItemStack itemStack, int amount) {
        List<RecipeHolder<?>> list = getRecipesToAwardAndPopExperience(entity.level(), entity.position(), this.worldPosition, entity, itemStack, amount);
        entity.awardRecipes(list);
        for (RecipeHolder<?> recipe : list) {
            entity.triggerRecipeCrafted(recipe, this.items);
        }
        this.recipesUsed.clear();
        return list;
    }

    @WrapOperation(method = "createExperience", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"))
    private static void arclight$expEvent(ServerLevel level, Vec3 vec3, int amount, Operation<Void> original) {
        original.call(level, vec3, ArclightEventFactory.experienceToDrop(level, amount));
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
        return this.litTimeRemaining > 0;
    }

    public Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> getRecipesUsed() {
        return this.recipesUsed;
    }
}
