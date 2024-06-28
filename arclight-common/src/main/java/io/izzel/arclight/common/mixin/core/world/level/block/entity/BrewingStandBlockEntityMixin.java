package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.tileentity.TileEntityBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.block.BrewingStartEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.BrewingStandFuelEvent;
import org.bukkit.inventory.InventoryHolder;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin extends LockableBlockEntityMixin {

    // @formatter:off
    @Shadow private NonNullList<ItemStack> items;
    // @formatter:on

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = MAX_STACK;

    @Decorate(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private static void arclight$brewFuel(ItemStack stack, int count, Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity entity) throws Throwable {
        BrewingStandFuelEvent event = new BrewingStandFuelEvent(CraftBlock.at(level, pos), CraftItemStack.asCraftMirror(stack), 20);
        Bukkit.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            DecorationOps.cancel().invoke();
            return;
        } else {
            entity.fuel = event.getFuelPower();
            if (entity.fuel > 0 && event.isConsuming()) {
                DecorationOps.callsite().invoke(stack, count);
            }
        }
        DecorationOps.blackhole().invoke();
    }

    @Inject(method = "serverTick", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;ingredient:Lnet/minecraft/world/item/Item;"))
    private static void arclight$brewBegin(Level level, BlockPos pos, BlockState p_155288_, BrewingStandBlockEntity entity, CallbackInfo ci) {
        var event = new BrewingStartEvent(CraftBlock.at(level, pos), CraftItemStack.asCraftMirror(entity.getItem(3)), entity.brewTime);
        Bukkit.getPluginManager().callEvent(event);
        entity.brewTime = event.getTotalBrewTime();
    }

    @Decorate(method = "doBrew", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;potionBrewing()Lnet/minecraft/world/item/alchemy/PotionBrewing;"))
    private static PotionBrewing arclight$brewEvent(Level instance, Level level, BlockPos pos, NonNullList<ItemStack> stacks,
                                                    @Local(allocate = "brewResults") List<org.bukkit.inventory.ItemStack> brewResults) throws Throwable {
        var potionBrewing = (PotionBrewing) DecorationOps.callsite().invoke(instance);
        BrewingStandBlockEntity entity = ArclightCaptures.getTickingBlockEntity();
        InventoryHolder owner = entity == null ? null : ((TileEntityBridge) entity).bridge$getOwner();
        ItemStack ing = stacks.get(3);
        brewResults = new ArrayList<>(3);
        for (int i = 0; i < 3; ++i) {
            brewResults.add(i, CraftItemStack.asCraftMirror(potionBrewing.mix(ing, stacks.get(i))));
        }

        if (owner != null) {
            BrewEvent event = new BrewEvent(CraftBlock.at(level, pos), (org.bukkit.inventory.BrewerInventory) owner.getInventory(), brewResults, entity.fuel);
            org.bukkit.Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return (PotionBrewing) DecorationOps.cancel().invoke();
            }
        }
        return potionBrewing;
    }

    @SuppressWarnings("unchecked")
    @Decorate(method = "doBrew", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/core/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;"))
    private static <E> E arclight$applyResults(NonNullList<E> instance, int i, E object,
                                               @Local(allocate = "brewResults") List<org.bukkit.inventory.ItemStack> brewResults) throws Throwable {
        if (i < brewResults.size()) {
            object = (E) CraftItemStack.asNMSCopy(brewResults.get(i));
        } else {
            object = (E) ItemStack.EMPTY;
        }
        return (E) DecorationOps.callsite().invoke(instance, i, object);
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
