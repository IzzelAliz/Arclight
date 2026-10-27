package io.izzel.arclight.common.mixin.core.world.inventory;

import com.destroystokyo.paper.event.block.AnvilDamagedEvent;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.common.bridge.core.world.inventory.PrepareResultMenuBridge;
import io.izzel.arclight.common.bridge.core.world.inventory.AbstractContainerMenuBridge;
import io.izzel.arclight.common.bridge.core.world.inventory.AnvilMenuBridge;
import io.izzel.arclight.common.bridge.core.world.inventory.ContainerLevelAccessBridge;
import io.izzel.arclight.common.mod.server.world.inventory.ArclightAnvilView;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftInventoryAnvil;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.view.CraftAnvilView;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiConsumer;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenuMixin implements AnvilMenuBridge {

    // @formatter:off
    @Shadow @Final public DataSlot cost;
    @Shadow public int repairItemCountCost;
    @Shadow public String itemName;
    @Shadow public static int calculateIncreasedRepairCost(int oldRepairCost) { return 0; }
    // @formatter:on

    public int cancelThisBySettingCostToMaximum = 40;
    public int maximumRenameCostThreshold = 40;
    public int maximumAllowedRenameCost = 39;
    public int maximumRepairCost = 40;

    private CraftAnvilView bukkitEntity;

    private boolean arclight$zeroCostAllowed = false;

    @Unique private static AnvilMenu arclight$damageMenu;

    // The native damage callback is static on both loaders. Carry its owning menu only
    // across the actual access call, leaving the callback and NeoForge pre/post hooks intact.
    @Decorate(method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ContainerLevelAccess;execute(Ljava/util/function/BiConsumer;)V"), require = 1, allow = 1)
    private void arclight$damageMenuContext(ContainerLevelAccess access, BiConsumer<Level, BlockPos> action) throws Throwable {
        AnvilMenu previous = arclight$damageMenu;
        arclight$damageMenu = (AnvilMenu) (Object) this;
        try {
            DecorationOps.callsite().invoke(access, action);
        } finally {
            arclight$damageMenu = previous;
        }
    }

    @Decorate(method = "lambda$onTake$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/AnvilBlock;damage(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"), require = 1, allow = 1)
    private static BlockState arclight$anvilDamaged(BlockState state) throws Throwable {
        BlockState damaged = (BlockState) DecorationOps.callsite().invoke(state);
        AnvilMenu menu = arclight$damageMenu;
        if (menu == null) {
            return damaged;
        }
        var event = new AnvilDamagedEvent(((AbstractContainerMenuBridge) menu).bridge$getBukkitView(),
            damaged == null ? null : CraftBlockData.createData(damaged));
        if (!event.callEvent()) {
            // Cancel only the damage callback: costs/inputs have already been consumed,
            // and the enclosing onTake must still reach NeoForge's crafting-post hook.
            return (BlockState) DecorationOps.cancel().invoke();
        }
        if (event.getDamageState() == AnvilDamagedEvent.DamageState.BROKEN) {
            return null;
        }
        return ((CraftBlockData) event.getDamageState().getMaterial().createBlockData()).getState()
            .setValue(AnvilBlock.FACING, state.getValue(AnvilBlock.FACING));
    }

    @Override
    public void arclight$allowZeroCost() {
        arclight$zeroCostAllowed = true;
    }

    @Override
    public boolean arclight$isZeroCostAllowed() {
        return arclight$zeroCostAllowed;
    }

    // NeoForge moves the vanilla result calculation into createResultInternal before running its anvil hook.
    // Match the result write across both layouts so Bukkit can still rewrite/cancel it without suppressing that hook.
    @Decorate(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$prepareAnvilEvent(ResultContainer instance, int i, ItemStack itemStack) throws Throwable {
        arclight$zeroCostAllowed = false;
        final CraftAnvilView craft = getBukkitView();
        if (craft instanceof ArclightAnvilView) {
            // Call anvil event; preserve injection point
            if (!((PrepareResultMenuBridge) this).bridge$isPrepareResultDeferred()) {
                var event = new PrepareAnvilEvent(craft, CraftItemStack.asCraftMirror(itemStack).clone());
                Bukkit.getServer().getPluginManager().callEvent(event);
                itemStack = CraftItemStack.asNMSCopy(event.getResult());
            }
            DecorationOps.callsite().invoke(instance, i, itemStack);
        } else {
            // Run plugin custom logic
            CraftEventFactory.callPrepareAnvilEvent(craft, itemStack);
            if (!((PrepareResultMenuBridge) this).bridge$isPrepareResultDeferred()) {
                CraftEventFactory.callPrepareResultEvent((AnvilMenu) (Object) this, AnvilMenu.RESULT_SLOT);
            }
        }
    }

    @WrapMethod(method = "setItemName(Ljava/lang/String;)Z", require = 1, allow = 1)
    private boolean arclight$prepareRenamedResult(String name, Operation<Boolean> original) {
        PrepareResultMenuBridge bridge = (PrepareResultMenuBridge) this;
        boolean previouslyDeferred = bridge.bridge$isPrepareResultDeferred();
        bridge.bridge$setPrepareResultDeferred(true);
        try {
            boolean changed = original.call(name);
            if (changed && !previouslyDeferred) {
                CraftEventFactory.callPrepareResultEvent((AnvilMenu) (Object) this, AnvilMenu.RESULT_SLOT);
            }
            return changed;
        } finally {
            bridge.bridge$setPrepareResultDeferred(previouslyDeferred);
        }
    }

    @Decorate(method = "mayPickup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/DataSlot;get()I"))
    private int arclight$tryAllowZeroCost(DataSlot instance) throws Throwable {
        final int value = (int) DecorationOps.callsite().invoke(instance);
        if (value == 0 && arclight$zeroCostAllowed) {
            return 1;
        }
        return value;
    }

    @Inject(method = "mayPickup", at = @At("RETURN"), cancellable = true)
    private void arclight$considerFlag(Player player, boolean hasItem, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(cir.getReturnValueZ() && hasItem);
    }

    @Inject(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;broadcastChanges()V"))
    private void arclight$sync(CallbackInfo ci) {
        if (!this.arclight$isSuppressingRemoteUpdates()) {
            this.sendAllDataToRemote();
        }
    }

    @ModifyConstant(method = "createResult*", constant = @Constant(intValue = 40), require = 0)
    private int arclight$maximumRepairCost(int i) {
        return i - 40 + maximumRepairCost;
    }

    @ModifyConstant(method = "createResult*", constant = @Constant(intValue = 39), require = 0)
    private int arclight$maximumRepairCost2(int i) {
        return i - 40 + maximumRepairCost;
    }

    @Override
    public CraftAnvilView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        var inventory = new CraftInventoryAnvil(
            ((ContainerLevelAccessBridge) this.access).bridge$getLocation(), this.inputSlots, this.resultSlots);
        bukkitEntity = new ArclightAnvilView(((PlayerBridge) this.player).bridge$getBukkitEntity(), inventory, (AnvilMenu) (Object) this);
        return bukkitEntity;
    }
}
