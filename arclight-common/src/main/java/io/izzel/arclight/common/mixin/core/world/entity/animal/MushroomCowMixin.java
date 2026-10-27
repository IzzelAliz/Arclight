package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@io.izzel.arclight.common.mod.mixins.annotation.OnlyInPlatform({io.izzel.arclight.api.ArclightPlatform.VANILLA, io.izzel.arclight.api.ArclightPlatform.FABRIC, io.izzel.arclight.api.ArclightPlatform.NEOFORGE})
@Mixin(MushroomCow.class)
public abstract class MushroomCowMixin extends AnimalMixin {

    @Unique private List<ItemStack> arclight$shearDrops;
    @Unique private ServerLevel arclight$shearLevel;
    @Unique private ItemInstance arclight$shearTool;

    @Decorate(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/cow/MushroomCow;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$onShear(MushroomCow cow, ServerLevel level, SoundSource source, ItemStack tool, Player player, InteractionHand hand) throws Throwable {
        List<ItemStack> defaultDrops = new ArrayList<>();
        this.dropFromShearingLootTable(level, BuiltInLootTables.SHEAR_MOOSHROOM, tool, (ignored, drop) -> {
            for (int i = 0; i < drop.getCount(); i++) {
                defaultDrops.add(drop.copyWithCount(1));
            }
        });
        var event = CraftEventFactory.handlePlayerShearEntityEvent(player, cow, tool, hand, defaultDrops);
        if (event != null && event.isCancelled()) {
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.CONSUME);
            return;
        }

        List<ItemStack> drops = event == null ? defaultDrops : CraftItemStack.asNMSCopy(event.getDrops());
        List<ItemStack> previousDrops = this.arclight$shearDrops;
        ServerLevel previousLevel = this.arclight$shearLevel;
        ItemInstance previousTool = this.arclight$shearTool;
        this.arclight$shearDrops = drops;
        this.arclight$shearLevel = level;
        this.arclight$shearTool = tool;
        try {
            DecorationOps.callsite().invoke(cow, level, source, tool);
        } finally {
            this.arclight$shearDrops = previousDrops;
            this.arclight$shearLevel = previousLevel;
            this.arclight$shearTool = previousTool;
        }
    }

    @Decorate(method = {"lambda$shear$0", "lambda$shear$1"}, require = 1, allow = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/cow/MushroomCow;dropFromShearingLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/item/ItemInstance;Ljava/util/function/BiConsumer;)V"))
    private void arclight$dropShearedItems(MushroomCow cow, ServerLevel level, ResourceKey<LootTable> lootTable, ItemInstance tool, BiConsumer<ServerLevel, ItemStack> dropper) throws Throwable {
        List<ItemStack> drops = null;
        if (this.arclight$shearLevel == level && this.arclight$shearTool == tool && BuiltInLootTables.SHEAR_MOOSHROOM.equals(lootTable)) {
            drops = this.arclight$shearDrops;
            this.arclight$shearDrops = List.of();
        }
        boolean previousForceDrops = this.bridge$isForceDrops();
        this.bridge$setForceDrops(true);
        try {
            if (drops == null) {
                DecorationOps.callsite().invoke(cow, level, lootTable, tool, dropper);
            } else {
                for (ItemStack drop : drops) {
                    if (!drop.isEmpty()) {
                        dropper.accept(level, drop);
                    }
                }
            }
        } finally {
            this.bridge$setForceDrops(previousForceDrops);
        }
    }
}
