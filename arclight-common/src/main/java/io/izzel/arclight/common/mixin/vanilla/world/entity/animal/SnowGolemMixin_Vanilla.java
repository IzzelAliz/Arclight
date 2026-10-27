package io.izzel.arclight.common.mixin.vanilla.world.entity.animal;

import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@Mixin(SnowGolem.class)
public abstract class SnowGolemMixin_Vanilla extends PathfinderMobMixin {

    // Forge/NeoForge: ShearsItem#interactLivingEntity
    @Decorate(method = "mobInteract", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/golem/SnowGolem;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$onShear(SnowGolem snowGolem, ServerLevel level, SoundSource soundSource, ItemStack stack, Player player, InteractionHand hand) throws Throwable {
        List<ItemStack> defaultDrops = new ArrayList<>();
        this.dropFromShearingLootTable(level, BuiltInLootTables.SHEAR_SNOW_GOLEM, stack, (ignored, drop) -> defaultDrops.add(drop));
        var event = CraftEventFactory.handlePlayerShearEntityEvent(player, snowGolem, stack, hand, defaultDrops);
        if (event != null && event.isCancelled()) {
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.PASS);
            return;
        }

        List<ItemStack> drops = event == null ? defaultDrops : CraftItemStack.asNMSCopy(event.getDrops());
        var previous = ArclightCaptures.snowGolemShearDrops;
        ArclightCaptures.snowGolemShearDrops = org.apache.commons.lang3.tuple.Triple.of(snowGolem, stack, drops);
        try {
            DecorationOps.callsite().invoke(snowGolem, level, soundSource, stack);
        } finally {
            ArclightCaptures.snowGolemShearDrops = previous;
        }
    }
}
