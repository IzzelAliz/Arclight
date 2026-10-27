package io.izzel.arclight.forge.mixin.core.world.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.common.IForgeShearable;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ShearsItem.class)
public class ShearsItemMixin_Forge {

    @WrapOperation(method = "interactLivingEntity(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/IForgeShearable;onSheared(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;I)Ljava/util/List;", remap = false),
        require = 1, allow = 1)
    private List<ItemStack> arclight$onShear(IForgeShearable target, Player player, ItemStack tool, Level world,
            BlockPos pos, int fortune, Operation<List<ItemStack>> original,
            @Local(argsOnly = true) LivingEntity entity, @Local(argsOnly = true) InteractionHand hand,
            @Cancellable CallbackInfoReturnable<InteractionResult> cir) {
        ResourceKey<LootTable> table = null;
        Class<?> type = entity.getClass();
        if (type == Sheep.class) table = BuiltInLootTables.SHEAR_SHEEP;
        else if (type == MushroomCow.class) table = BuiltInLootTables.SHEAR_MOOSHROOM;
        else if (type == SnowGolem.class) table = BuiltInLootTables.SHEAR_SNOW_GOLEM;
        else if (type == Bogged.class) table = BuiltInLootTables.BOGGED_SHEAR;
        boolean copper = type == CopperGolem.class;
        boolean previewed = table != null || copper;
        List<ItemStack> drops = new ArrayList<>();
        if (table != null && world instanceof ServerLevel level) {
            var params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, entity.position())
                .withParameter(LootContextParams.THIS_ENTITY, entity)
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.SHEARING);
            for (ItemStack drop : level.getServer().reloadableRegistries().getLootTable(table).getRandomItems(params)) {
                drops.add(drop.copy());
            }
        } else if (copper) {
            drops.add(entity.getItemBySlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA).copy());
        }
        var event = CraftEventFactory.handlePlayerShearEntityEvent(player, entity, tool, hand,
            drops.stream().map(ItemStack::copy).toList());
        if (event != null && event.isCancelled()) {
            cir.setReturnValue(InteractionResult.PASS);
            return List.of();
        }
        if (!previewed) {
            // A mod's onSheared can mutate its entity; never invoke it to preview drops.
            return original.call(target, player, tool, world, pos, fortune);
        }
        List<ItemStack> selected = event == null ? drops : CraftItemStack.asNMSCopy(event.getDrops());
        var previous = ArclightCaptures.forgeShearDrops;
        ArclightCaptures.forgeShearDrops = table == null ? null : Triple.of(entity, tool, Pair.of(table, drops));
        try {
            List<ItemStack> nativeDrops = original.call(target, player, tool, world, pos, fortune);
            boolean consumed = ArclightCaptures.forgeShearDrops == null;
            if ((!copper && !consumed) || (type == MushroomCow.class && !entity.isRemoved())) {
                return nativeDrops;
            }
            return selected;
        } finally {
            ArclightCaptures.forgeShearDrops = previous;
        }
    }
}
