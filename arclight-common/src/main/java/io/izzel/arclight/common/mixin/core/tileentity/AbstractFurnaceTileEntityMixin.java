package io.izzel.arclight.common.mixin.core.tileentity;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.tileentity.AbstractFurnaceTileEntityBridge;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.util.CraftMagicNumbers;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceTileEntityMixin extends LockableTileEntityMixin implements AbstractFurnaceTileEntityBridge {

    // @formatter:off
    @Shadow protected NonNullList<ItemStack> items;
    @Shadow protected abstract int getBurnDuration(ItemStack stack);
    @Shadow public int litTime;
    @Shadow protected abstract boolean isLit();
    @Shadow protected abstract boolean canBurn(@Nullable Recipe<?> recipeIn);
    @Shadow public abstract void setRecipeUsed(@Nullable Recipe<?> recipe);
    @Shadow public abstract List<Recipe<?>> getRecipesToAwardAndPopExperience(Level world, Vec3 pos);
    @Shadow @Final private Object2IntOpenHashMap<ResourceLocation> recipesUsed;
    // @formatter:on

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = MAX_STACK;

    private transient FurnaceBurnEvent arclight$burnEvent;

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getBurnDuration(Lnet/minecraft/world/item/ItemStack;)I"))
    public void arclight$furnaceBurn(CallbackInfo ci) {
        ItemStack itemStack = this.items.get(1);
        CraftItemStack fuel = CraftItemStack.asCraftMirror(itemStack);

        arclight$burnEvent = new FurnaceBurnEvent(CraftBlock.at(this.level, this.worldPosition), fuel, getBurnDuration(itemStack));
        Bukkit.getPluginManager().callEvent(arclight$burnEvent);

        if (arclight$burnEvent.isCancelled()) {
            ci.cancel();
            arclight$burnEvent = null;
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 4, target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;isLit()Z"))
    public boolean arclight$setBurnTime(AbstractFurnaceBlockEntity furnace) {
        this.litTime = arclight$burnEvent.getBurnTime();
        try {
            return this.isLit() && arclight$burnEvent.isBurning();
        } finally {
            arclight$burnEvent = null;
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    @SuppressWarnings("unchecked")
    private void burn(@Nullable Recipe<?> recipe) {
        if (recipe != null && this.canBurn(recipe)) {
            ItemStack itemstack = this.items.get(0);
            ItemStack itemstack1 = ((Recipe<WorldlyContainer>) recipe).assemble((AbstractFurnaceBlockEntity)(Object)this);
            ItemStack itemstack2 = this.items.get(2);

            CraftItemStack source = CraftItemStack.asCraftMirror(itemstack);
            org.bukkit.inventory.ItemStack result = CraftItemStack.asBukkitCopy(itemstack1);

            FurnaceSmeltEvent event = new FurnaceSmeltEvent(CraftBlock.at(level, worldPosition), source, result);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return;
            }

            result = event.getResult();
            itemstack1 = CraftItemStack.asNMSCopy(result);

            if (!itemstack1.isEmpty()) {
                if (itemstack2.isEmpty()) {
                    this.items.set(2, itemstack1.copy());
                } else if (CraftItemStack.asCraftMirror(itemstack2).isSimilar(result)) {
                    itemstack2.grow(itemstack1.getCount());
                } else {
                    return;
                }
            }

            if (!this.level.isClientSide) {
                this.setRecipeUsed(recipe);
            }

            if (itemstack.getItem() == Blocks.WET_SPONGE.asItem() && !this.items.get(1).isEmpty() && this.items.get(1).getItem() == Items.BUCKET) {
                this.items.set(1, new ItemStack(Items.WATER_BUCKET));
            }

            itemstack.shrink(1);
        }
    }

    private static AbstractFurnaceBlockEntity arclight$captureFurnace;
    private static Player arclight$capturePlayer;
    private static ItemStack arclight$item;
    private static int arclight$captureAmount;

    public List<Recipe<?>> a(Level world, Vec3 pos, Player entity, ItemStack itemStack, int amount) {
        try {
            arclight$item = itemStack;
            arclight$captureAmount = amount;
            arclight$captureFurnace = (AbstractFurnaceBlockEntity) (Object) this;
            arclight$capturePlayer = entity;
            List<Recipe<?>> list = this.getRecipesToAwardAndPopExperience(world, pos);
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
    public List<Recipe<?>> bridge$dropExp(Level world, Vec3 pos, Player entity, ItemStack itemStack, int amount) {
        return a(world, pos, entity, itemStack, amount);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private static void createExperience(Level world, Vec3 pos, int craftedAmount, float experience) {
        int i = Mth.floor((float) craftedAmount * experience);
        float f = Mth.frac((float) craftedAmount * experience);
        if (f != 0.0F && Math.random() < (double) f) {
            ++i;
        }

        if (arclight$capturePlayer != null && arclight$captureAmount != 0) {
            FurnaceExtractEvent event = new FurnaceExtractEvent(((ServerPlayerEntityBridge) arclight$capturePlayer).bridge$getBukkitEntity(),
                CraftBlock.at(world, arclight$captureFurnace.getBlockPos()), CraftMagicNumbers.getMaterial(arclight$item.getItem()), arclight$captureAmount, i);
            Bukkit.getPluginManager().callEvent(event);
            i = event.getExpToDrop();
        }

        while (i > 0) {
            int j = ExperienceOrb.getExperienceValue(i);
            i -= j;
            world.addFreshEntity(new ExperienceOrb(world, pos.x, pos.y, pos.z, j));
        }
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
}
