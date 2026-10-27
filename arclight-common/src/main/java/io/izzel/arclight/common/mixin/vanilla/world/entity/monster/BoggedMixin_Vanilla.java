package io.izzel.arclight.common.mixin.vanilla.world.entity.monster;

import io.izzel.arclight.common.bridge.core.network.syncher.SynchedEntityDataBridge;
import io.izzel.arclight.common.mixin.core.world.entity.monster.AbstractSkeletonMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@Mixin(Bogged.class)
public abstract class BoggedMixin_Vanilla extends AbstractSkeletonMixin {

    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_SHEARED;

    @Unique private List<ItemStack> arclight$shearDrops;
    @Unique private ServerLevel arclight$shearLevel;
    @Unique private ItemInstance arclight$shearTool;

    @Decorate(method = "mobInteract", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/skeleton/Bogged;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$shearEvent(Bogged bogged, ServerLevel level, SoundSource soundSource, ItemStack stack, Player player, InteractionHand hand) throws Throwable {
        List<ItemStack> defaultDrops = new ArrayList<>();
        this.dropFromShearingLootTable(level, BuiltInLootTables.BOGGED_SHEAR, stack, (ignored, drop) -> defaultDrops.add(drop));
        var event = CraftEventFactory.handlePlayerShearEntityEvent(player, bogged, stack, hand, defaultDrops);
        if (event != null && event.isCancelled()) {
            ((SynchedEntityDataBridge) this.getEntityData()).bridge$markDirty(DATA_SHEARED);
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.PASS);
            return;
        }

        List<ItemStack> drops = event == null ? defaultDrops : CraftItemStack.asNMSCopy(event.getDrops());
        List<ItemStack> previousDrops = this.arclight$shearDrops;
        ServerLevel previousLevel = this.arclight$shearLevel;
        ItemInstance previousTool = this.arclight$shearTool;
        this.arclight$shearDrops = drops;
        this.arclight$shearLevel = level;
        this.arclight$shearTool = stack;
        try {
            DecorationOps.callsite().invoke(bogged, level, soundSource, stack);
        } finally {
            this.arclight$shearDrops = previousDrops;
            this.arclight$shearLevel = previousLevel;
            this.arclight$shearTool = previousTool;
        }
    }

    @Decorate(method = "spawnShearedMushrooms", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/skeleton/Bogged;dropFromShearingLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/item/ItemInstance;Ljava/util/function/BiConsumer;)V"))
    private void arclight$shearDrops(Bogged bogged, ServerLevel level, ResourceKey<LootTable> lootTable, ItemInstance tool, BiConsumer<ServerLevel, ItemStack> dropper) throws Throwable {
        List<ItemStack> drops = null;
        if (this.arclight$shearLevel == level && this.arclight$shearTool == tool && BuiltInLootTables.BOGGED_SHEAR.equals(lootTable)) {
            drops = this.arclight$shearDrops;
            this.arclight$shearDrops = null;
        }
        boolean previous = this.bridge$isForceDrops();
        this.bridge$setForceDrops(true);
        try {
            if (drops == null) {
                DecorationOps.callsite().invoke(bogged, level, lootTable, tool, dropper);
            } else {
                // Consume the event selection through the native shear dropper, without a second loot roll.
                for (ItemStack drop : drops) {
                    dropper.accept(level, drop);
                }
            }
        } finally {
            this.bridge$setForceDrops(previous);
        }
    }
}
