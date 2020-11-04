package io.izzel.arclight.common.mixin.core.tileentity;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.tileentity.AbstractFurnaceTileEntityBridge;
import net.minecraft.block.Blocks;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.AbstractFurnaceTileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractFurnaceTileEntity.class)
public abstract class AbstractFurnaceTileEntityMixin extends LockableTileEntityMixin implements AbstractFurnaceTileEntityBridge {

    // @formatter:off
    @Shadow protected NonNullList<ItemStack> items;
    @Shadow protected abstract int getBurnTime(ItemStack stack);
    @Shadow public int burnTime;
    @Shadow protected abstract boolean isBurning();
    @Shadow protected abstract boolean canSmelt(@Nullable IRecipe<?> recipeIn);
    @Shadow public abstract void setRecipeUsed(@Nullable IRecipe<?> recipe);
    @Shadow public abstract List<IRecipe<?>> grantStoredRecipeExperience(World world, Vector3d pos);
    // @formatter:on

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = MAX_STACK;

    private transient FurnaceBurnEvent arclight$burnEvent;

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/AbstractFurnaceTileEntity;getBurnTime(Lnet/minecraft/item/ItemStack;)I"))
    public void arclight$furnaceBurn(CallbackInfo ci) {
        ItemStack itemStack = this.items.get(1);
        CraftItemStack fuel = CraftItemStack.asCraftMirror(itemStack);

        arclight$burnEvent = new FurnaceBurnEvent(CraftBlock.at(this.world, this.pos), fuel, getBurnTime(itemStack));
        Bukkit.getPluginManager().callEvent(arclight$burnEvent);

        if (arclight$burnEvent.isCancelled()) {
            ci.cancel();
            arclight$burnEvent = null;
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 4, target = "Lnet/minecraft/tileentity/AbstractFurnaceTileEntity;isBurning()Z"))
    public boolean arclight$setBurnTime(AbstractFurnaceTileEntity furnace) {
        this.burnTime = arclight$burnEvent.getBurnTime();
        try {
            return this.isBurning() && arclight$burnEvent.isBurning();
        } finally {
            arclight$burnEvent = null;
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void smelt(@Nullable IRecipe<?> recipe) {
        if (recipe != null && this.canSmelt(recipe)) {
            ItemStack itemstack = this.items.get(0);
            ItemStack itemstack1 = recipe.getRecipeOutput();
            ItemStack itemstack2 = this.items.get(2);

            CraftItemStack source = CraftItemStack.asCraftMirror(itemstack);
            org.bukkit.inventory.ItemStack result = CraftItemStack.asBukkitCopy(itemstack1);

            FurnaceSmeltEvent event = new FurnaceSmeltEvent(CraftBlock.at(world, pos), source, result);
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

            if (!this.world.isRemote) {
                this.setRecipeUsed(recipe);
            }

            if (itemstack.getItem() == Blocks.WET_SPONGE.asItem() && !this.items.get(1).isEmpty() && this.items.get(1).getItem() == Items.BUCKET) {
                this.items.set(1, new ItemStack(Items.WATER_BUCKET));
            }

            itemstack.shrink(1);
        }
    }

    private static AbstractFurnaceTileEntity arclight$captureFurnace;
    private static PlayerEntity arclight$capturePlayer;
    private static ItemStack arclight$item;
    private static int arclight$captureAmount;

    public List<IRecipe<?>> a(World world, Vector3d pos, PlayerEntity entity, ItemStack itemStack, int amount) {
        try {
            arclight$item = itemStack;
            arclight$captureAmount = amount;
            arclight$captureFurnace = (AbstractFurnaceTileEntity) (Object) this;
            arclight$capturePlayer = entity;
            return this.grantStoredRecipeExperience(world, pos);
        } finally {
            arclight$item = null;
            arclight$captureAmount = 0;
            arclight$captureFurnace = null;
            arclight$capturePlayer = null;
        }
    }

    @Override
    public List<IRecipe<?>> bridge$dropExp(World world, Vector3d pos, PlayerEntity entity, ItemStack itemStack, int amount) {
        return a(world, pos, entity, itemStack, amount);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private static void splitAndSpawnExperience(World world, Vector3d pos, int craftedAmount, float experience) {
        int i = MathHelper.floor((float) craftedAmount * experience);
        float f = MathHelper.frac((float) craftedAmount * experience);
        if (f != 0.0F && Math.random() < (double) f) {
            ++i;
        }

        if (arclight$capturePlayer != null && arclight$captureAmount != 0) {
            FurnaceExtractEvent event = new FurnaceExtractEvent(((ServerPlayerEntityBridge) arclight$capturePlayer).bridge$getBukkitEntity(),
                CraftBlock.at(world, arclight$captureFurnace.getPos()), CraftMagicNumbers.getMaterial(arclight$item.getItem()), arclight$captureAmount, i);
            Bukkit.getPluginManager().callEvent(event);
            i = event.getExpToDrop();
        }

        while (i > 0) {
            int j = ExperienceOrbEntity.getXPSplit(i);
            i -= j;
            world.addEntity(new ExperienceOrbEntity(world, pos.x, pos.y, pos.z, j));
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
    public int getInventoryStackLimit() {
        if (maxStack == 0) maxStack = MAX_STACK;
        return maxStack;
    }

    @Override
    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }
}
