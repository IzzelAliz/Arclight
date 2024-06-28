package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.tileentity.AbstractFurnaceTileEntityBridge;
import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeHolderBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
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
import org.bukkit.event.inventory.FurnaceSmeltEvent;
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
public abstract class AbstractFurnaceBlockEntityMixin extends LockableBlockEntityMixin implements AbstractFurnaceTileEntityBridge {

    // @formatter:off
    @Shadow protected NonNullList<ItemStack> items;
    @Shadow protected abstract int getBurnDuration(ItemStack stack);
    @Shadow protected abstract boolean isLit();
    @Shadow @Final private Object2IntOpenHashMap<ResourceLocation> recipesUsed;
    @Shadow public abstract List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel p_154996_, Vec3 p_154997_);
    // @formatter:on

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = MAX_STACK;

    @SuppressWarnings("unchecked")
    @Decorate(method = "burn", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/core/NonNullList;get(I)Ljava/lang/Object;"))
    private static <E> E arclight$furnaceSmelt(NonNullList<E> instance, int i, @Local(ordinal = -1) ItemStack itemStack2, @Local(ordinal = -2) ItemStack itemStack1) throws Throwable {
        var blockEntity = ArclightCaptures.getTickingBlockEntity();
        if (blockEntity != null) {
            CraftItemStack source = CraftItemStack.asCraftMirror(itemStack1);
            org.bukkit.inventory.ItemStack result = CraftItemStack.asBukkitCopy(itemStack2);

            FurnaceSmeltEvent furnaceSmeltEvent = new FurnaceSmeltEvent(CraftBlock.at(blockEntity.getLevel(), blockEntity.getBlockPos()), source, result);
            Bukkit.getPluginManager().callEvent(furnaceSmeltEvent);

            if (furnaceSmeltEvent.isCancelled()) {
                return (E) DecorationOps.cancel().invoke(false);
            }

            result = furnaceSmeltEvent.getResult();
            itemStack2 = CraftItemStack.asNMSCopy(result);
            if (itemStack2.isEmpty()) {
                itemStack1.shrink(1);
                return (E) DecorationOps.cancel().invoke(true);
            }
        }
        return (E) DecorationOps.callsite().invoke(instance, i);
    }

    @Decorate(method = "serverTick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;isLit()Z"),
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;litDuration:I")))
    private static boolean arclight$setBurnTime(AbstractFurnaceBlockEntity furnace) throws Throwable {
        ItemStack itemStack = furnace.getItem(1);
        CraftItemStack fuel = CraftItemStack.asCraftMirror(itemStack);
        FurnaceBurnEvent furnaceBurnEvent = new FurnaceBurnEvent(CraftBlock.at(furnace.level, furnace.getBlockPos()), fuel, furnace.litTime);
        Bukkit.getPluginManager().callEvent(furnaceBurnEvent);
        if (furnaceBurnEvent.isCancelled()) {
            return (boolean) DecorationOps.cancel().invoke();
        }
        furnace.litTime = furnaceBurnEvent.getBurnTime();
        return (boolean) DecorationOps.callsite().invoke(furnace) && furnaceBurnEvent.isBurning();
    }

    @Decorate(method = "serverTick", inject = true, at = @At(value = "FIELD", ordinal = 0, target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;cookingProgress:I"))
    private static void arclight$startSmelt(Level level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity furnace,
                                            @Local(ordinal = -1) RecipeHolder<?> recipe) {
        if (recipe != null && furnace.cookingProgress == 0) {
            CraftItemStack source = CraftItemStack.asCraftMirror(furnace.getItem(0));
            if (((RecipeHolderBridge) (Object) recipe).bridge$toBukkitRecipe() instanceof CookingRecipe<?> cookingRecipe) {
                FurnaceStartSmeltEvent event = new FurnaceStartSmeltEvent(CraftBlock.at(level, pos), source, cookingRecipe);
                Bukkit.getPluginManager().callEvent(event);
                furnace.cookingTotalTime = event.getTotalCookTime();
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
        return getRecipesToAwardAndPopExperience(entity.serverLevel(), entity.position(), this.worldPosition, entity, itemStack, amount);
    }

    @Redirect(method = "createExperience", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"))
    private static void arclight$expEvent(ServerLevel level, Vec3 vec3, int amount) {
        if (arclight$capturePlayer != null && arclight$captureAmount != 0) {
            FurnaceExtractEvent event = new FurnaceExtractEvent(((ServerPlayerEntityBridge) arclight$capturePlayer).bridge$getBukkitEntity(),
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
        return this.getBurnDuration(stack);
    }

    @Override
    public boolean bridge$isLit() {
        return this.isLit();
    }

    public Object2IntOpenHashMap<ResourceLocation> getRecipesUsed() {
        return this.recipesUsed;
    }
}
