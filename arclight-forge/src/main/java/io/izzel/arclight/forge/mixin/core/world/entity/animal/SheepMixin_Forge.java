package io.izzel.arclight.forge.mixin.core.world.entity.animal;

import com.mojang.datafixers.util.Pair;
import io.izzel.arclight.common.mixin.core.world.entity.animal.AnimalMixin;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Sheep.class)
public abstract class SheepMixin_Forge extends AnimalMixin {

    @Decorate(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/sheep/Sheep;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"), require = 1, allow = 1)
    private void arclight$playerShear(Sheep sheep, ServerLevel level, SoundSource source,
            ItemStack tool, Player player, InteractionHand hand) throws Throwable {
        List<ItemStack> defaults = new ArrayList<>();
        this.dropFromShearingLootTable(level, BuiltInLootTables.SHEAR_SHEEP,
            tool, (ignored, drop) -> defaults.add(drop.copy()));
        var event = CraftEventFactory.handlePlayerShearEntityEvent(player, sheep, tool, hand, defaults);
        if (event != null && event.isCancelled()) {
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.CONSUME);
            return;
        }
        var selected = event == null ? defaults : CraftItemStack.asNMSCopy(event.getDrops());
        var previous = ArclightCaptures.forgeShearDrops;
        ArclightCaptures.forgeShearDrops = Triple.of(
            sheep, tool, Pair.of(BuiltInLootTables.SHEAR_SHEEP, selected));
        try {
            DecorationOps.callsite().invoke(sheep, level, source, tool);
        } finally {
            ArclightCaptures.forgeShearDrops = previous;
        }
    }

    @Decorate(method = "lambda$shear$0(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/sheep/Sheep;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;"),
        require = 1, expect = 1, allow = 1)
    private ItemEntity arclight$forceDrop(Sheep sheep, ServerLevel level, ItemStack stack, float offset) throws Throwable {
        boolean previous = this.bridge$isForceDrops();
        this.bridge$setForceDrops(true);
        try {
            return (ItemEntity) DecorationOps.callsite().invoke(sheep, level, stack, offset);
        } finally {
            this.bridge$setForceDrops(previous);
        }
    }
}