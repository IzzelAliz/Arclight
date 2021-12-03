package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.item.crafting.IRecipeBridge;
import io.izzel.arclight.common.bridge.core.tileentity.AbstractFurnaceTileEntityBridge;
import io.izzel.arclight.mixin.Eject;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.util.CraftMagicNumbers;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin extends LockableBlockEntityMixin implements AbstractFurnaceTileEntityBridge {

    // @formatter:off
    @Shadow protected NonNullList<ItemStack> items;
    @Shadow protected abstract int getBurnDuration(ItemStack stack);
    @Shadow protected abstract boolean isLit();
    @Shadow @Final private Object2IntOpenHashMap<ResourceLocation> recipesUsed;
    @Shadow public abstract List<Recipe<?>> getRecipesToAwardAndPopExperience(ServerLevel p_154996_, Vec3 p_154997_);
    @Shadow protected abstract boolean canBurn(@org.jetbrains.annotations.Nullable Recipe<?> p_155006_, NonNullList<ItemStack> p_155007_, int p_155008_);
    // @formatter:on

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = MAX_STACK;

    @Eject(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;isLit()Z"),
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;litDuration:I"),
            to = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/item/ItemStack;hasContainerItem()Z")))
    private static boolean arclight$setBurnTime(AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
        ItemStack itemStack = furnace.getItem(1);
        CraftItemStack fuel = CraftItemStack.asCraftMirror(itemStack);
        FurnaceBurnEvent furnaceBurnEvent = new FurnaceBurnEvent(CraftBlock.at(furnace.level, furnace.getBlockPos()), fuel, ((AbstractFurnaceTileEntityBridge) furnace).bridge$getBurnDuration(itemStack));
        Bukkit.getPluginManager().callEvent(furnaceBurnEvent);

        if (furnaceBurnEvent.isCancelled()) {
            ci.cancel();
            return false;
        }
        return ((AbstractFurnaceTileEntityBridge) furnace).bridge$isLit() && furnaceBurnEvent.isBurning();
    }

    @Inject(method = "serverTick", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "FIELD", ordinal = 0, target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;cookingProgress:I"))
    private static void arclight$startSmelt(Level level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity furnace, CallbackInfo ci,
                                            boolean flag, boolean flag1, ItemStack stack, Recipe<?> recipe) {
        if (recipe != null && furnace.cookingProgress == 0) {
            CraftItemStack source = CraftItemStack.asCraftMirror(furnace.getItem(0));
            CookingRecipe<?> cookingRecipe = (CookingRecipe<?>) ((IRecipeBridge) recipe).bridge$toBukkitRecipe();

            FurnaceStartSmeltEvent event = new FurnaceStartSmeltEvent(CraftBlock.at(level, pos), source, cookingRecipe);
            Bukkit.getPluginManager().callEvent(event);
            furnace.cookingTotalTime = event.getTotalCookTime();
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private boolean burn(@Nullable Recipe<?> recipe, NonNullList<ItemStack> items, int i) {
        if (recipe != null && this.canBurn(recipe, items, i)) {
            ItemStack itemstack = items.get(0);
            ItemStack itemstack1 = ((Recipe<WorldlyContainer>) recipe).assemble((AbstractFurnaceBlockEntity) (Object) this);
            ItemStack itemstack2 = items.get(2);
            CraftItemStack source = CraftItemStack.asCraftMirror(itemstack);
            org.bukkit.inventory.ItemStack result = CraftItemStack.asBukkitCopy(itemstack1);

            FurnaceSmeltEvent furnaceSmeltEvent = new FurnaceSmeltEvent(CraftBlock.at(level, worldPosition), source, result);
            Bukkit.getPluginManager().callEvent(furnaceSmeltEvent);

            if (furnaceSmeltEvent.isCancelled()) {
                return false;
            }

            result = furnaceSmeltEvent.getResult();
            itemstack1 = CraftItemStack.asNMSCopy(result);

            if (!itemstack1.isEmpty()) {
                if (itemstack2.isEmpty()) {
                    items.set(2, itemstack1.copy());
                } else if (CraftItemStack.asCraftMirror(itemstack2).isSimilar(result)) {
                    itemstack2.grow(itemstack1.getCount());
                } else {
                    return false;
                }
            }

            if (itemstack.is(Blocks.WET_SPONGE.asItem()) && !items.get(1).isEmpty() && items.get(1).is(Items.BUCKET)) {
                items.set(1, new ItemStack(Items.WATER_BUCKET));
            }

            itemstack.shrink(1);
            return true;
        } else {
            return false;
        }
    }

    private static AbstractFurnaceBlockEntity arclight$captureFurnace;
    private static Player arclight$capturePlayer;
    private static ItemStack arclight$item;
    private static int arclight$captureAmount;

    public List<Recipe<?>> getRecipesToAwardAndPopExperience(ServerLevel world, Vec3 vec, BlockPos pos, Player entity, ItemStack itemStack, int amount) {
        try {
            arclight$item = itemStack;
            arclight$captureAmount = amount;
            arclight$captureFurnace = (AbstractFurnaceBlockEntity) (Object) this;
            arclight$capturePlayer = entity;
            List<Recipe<?>> list = this.getRecipesToAwardAndPopExperience(world, vec);
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
    public List<Recipe<?>> bridge$dropExp(ServerPlayer entity, ItemStack itemStack, int amount) {
        return getRecipesToAwardAndPopExperience(entity.getLevel(), entity.position(), this.worldPosition, entity, itemStack, amount);
    }

    @Redirect(method = "createExperience", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"))
    private static void arclight$expEvent(ServerLevel level, Vec3 vec3, int amount) {
        if (arclight$capturePlayer != null && arclight$captureAmount != 0) {
            FurnaceExtractEvent event = new FurnaceExtractEvent(((ServerPlayerEntityBridge) arclight$capturePlayer).bridge$getBukkitEntity(),
                CraftBlock.at(level, arclight$captureFurnace.getBlockPos()), CraftMagicNumbers.getMaterial(arclight$item.getItem()), arclight$captureAmount, amount);
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
}
