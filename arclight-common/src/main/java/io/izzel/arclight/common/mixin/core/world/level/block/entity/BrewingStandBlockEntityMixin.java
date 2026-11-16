package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.world.level.block.entity.BlockEntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
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
public abstract class BrewingStandBlockEntityMixin extends BaseContainerBlockEntityMixin {

    // @formatter:off
    @Shadow private NonNullList<ItemStack> items;
    // @formatter:on

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = MAX_STACK;

    @Decorate(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;getUses(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/component/BrewingFuel;)I"))
    private static int arclight$brewFuel(BrewingStandBlockEntity entity, net.minecraft.server.level.ServerLevel level, net.minecraft.world.item.component.BrewingFuel brewingFuel,
                                         @Local(ordinal = 0) BlockPos pos, @Local(ordinal = 0) ItemStack fuel,
                                         @Local(allocate = "arclightBrewingFuel") BrewingStandFuelEvent event) throws Throwable {
        int uses = (int) DecorationOps.callsite().invoke(entity, level, brewingFuel);
        event = new BrewingStandFuelEvent(CraftBlock.at(level, pos), CraftItemStack.asCraftMirror(fuel), uses);
        Bukkit.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return (int) DecorationOps.cancel().invoke();
        }
        DecorationOps.blackhole().invoke(event);
        return event.getFuelPower();
    }

    @Decorate(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;getCraftingRemainder()Lnet/minecraft/world/item/ItemStackTemplate;"))
    private static net.minecraft.world.item.ItemStackTemplate arclight$fuelRemainder(net.minecraft.world.item.Item item,
                                                                                 @Local(ordinal = 0) BrewingStandBlockEntity entity,
                                                                                 @Local(allocate = "arclightBrewingFuel") BrewingStandFuelEvent event,
                                                                                 @Local(allocate = "arclightConsumeBrewingFuel") boolean consume) throws Throwable {
        var remainder = (net.minecraft.world.item.ItemStackTemplate) DecorationOps.callsite().invoke(item);
        consume = entity.fuel > 0 && event.isConsuming();
        DecorationOps.blackhole().invoke(consume);
        return consume ? remainder : null;
    }

    @Decorate(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private static void arclight$consumeFuel(ItemStack fuel, int count,
                                            @Local(allocate = "arclightConsumeBrewingFuel") boolean consume) throws Throwable {
        if (consume) {
            DecorationOps.callsite().invoke(fuel, count);
        }
    }

    @Inject(method = "serverTick", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;ingredient:Lnet/minecraft/world/item/Item;"))
    private static void arclight$brewBegin(net.minecraft.server.level.ServerLevel level, BlockPos pos, BlockState p_155288_, BrewingStandBlockEntity entity, CallbackInfo ci) {
        var event = new BrewingStartEvent(CraftBlock.at(level, pos), CraftItemStack.asCraftMirror(entity.getItem(3)), entity.brewTime);
        Bukkit.getPluginManager().callEvent(event);
        entity.brewTime = event.getBrewingTime();
        entity.totalBrewTime = event.getRecipeBrewTime();
    }

    @Decorate(method = "doBrew", inject = true, at = @At("HEAD"))
    private static void arclight$prepareBrewResults(@Local(allocate = "brewResults") List<org.bukkit.inventory.ItemStack> brewResults) throws Throwable {
        brewResults = new ArrayList<>(3);
        DecorationOps.blackhole().invoke(brewResults);
    }

    @SuppressWarnings("unchecked")
    @Decorate(method = "doBrew", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/core/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;"))
    private static <E> E arclight$applyResults(NonNullList<E> instance, int i, E object,
                                               @Local(ordinal = 0) net.minecraft.server.level.ServerLevel level,
                                               @Local(ordinal = 0) BlockPos pos,
                                               @Local(ordinal = 0) BrewingStandBlockEntity entity,
                                               @Local(allocate = "brewResults") List<org.bukkit.inventory.ItemStack> brewResults) throws Throwable {
        E previous = instance.get(i);
        brewResults.add(CraftItemStack.asCraftMirror((ItemStack) object));
        if (i != 2) {
            return previous;
        }
        InventoryHolder owner = ((BlockEntityBridge) entity).bridge$getOwner();
        if (owner != null) {
            BrewEvent event = new BrewEvent(CraftBlock.at(level, pos), (org.bukkit.inventory.BrewerInventory) owner.getInventory(), brewResults, entity.fuel);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return (E) DecorationOps.cancel().invoke();
            }
        }
        for (int slot = 0; slot < 3; ++slot) {
            E result = (E) (slot < brewResults.size() ? CraftItemStack.asNMSCopy(brewResults.get(slot)) : ItemStack.EMPTY);
            E replaced = (E) DecorationOps.callsite().invoke(instance, slot, result);
        }
        return previous;
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
