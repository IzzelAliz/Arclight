package io.izzel.arclight.forge.mixin.core.world.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.bridge.core.world.entity.ShearableBridge;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
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
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
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

    @org.spongepowered.asm.mixin.injection.Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
    private void arclight$cancelledCopperShear(ItemStack tool, Player player, LivingEntity entity,
                                              InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (((io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge) player).bridge$isCopperShearCancelled(entity)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @WrapOperation(method = "interactLivingEntity(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Shearable;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"),
        require = 1, allow = 1)
    private void arclight$onShear(Shearable target, ServerLevel level, SoundSource source, ItemStack tool,
            Operation<Void> original, @Local(argsOnly = true) Player player,
            @Local(argsOnly = true) LivingEntity entity, @Local(argsOnly = true) InteractionHand hand,
            @Cancellable CallbackInfoReturnable<InteractionResult> cir) {
        ResourceKey<LootTable> table = null;
        Class<?> type = entity.getClass();
        if (type == Sheep.class) table = BuiltInLootTables.SHEAR_SHEEP;
        else if (type == MushroomCow.class) table = BuiltInLootTables.SHEAR_MOOSHROOM;
        else if (type == SnowGolem.class) table = BuiltInLootTables.SHEAR_SNOW_GOLEM;
        else if (type == Bogged.class) table = BuiltInLootTables.BOGGED_SHEAR;
        boolean copper = type == CopperGolem.class;
        boolean sulfur = type == SulfurCube.class;
        List<ItemStack> drops = new ArrayList<>();
        if (table != null) {
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
        } else if (sulfur) {
            drops.add(entity.getItemBySlot(EquipmentSlot.BODY).copy());
        }
        var event = CraftEventFactory.handlePlayerShearEntityEvent(player, entity, tool, hand,
            drops.stream().map(ItemStack::copy).toList());
        if (event != null && event.isCancelled()) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }
        List<ItemStack> selected = event == null ? drops : CraftItemStack.asNMSCopy(event.getDrops());
        var previous = ArclightCaptures.forgeShearDrops;
        ShearableBridge equipment = copper || sulfur ? (ShearableBridge) entity : null;
        List<ItemStack> previousEquipment = equipment == null ? null : equipment.bridge$swapShearDrops(selected);
        ArclightCaptures.forgeShearDrops = table == null ? null : Triple.of(entity, tool, Pair.of(table, selected));
        try {
            // Unknown mod shear operations have no side-effect-free drop preview.
            original.call(target, level, source, tool);
        } finally {
            ArclightCaptures.forgeShearDrops = previous;
            if (equipment != null) {
                equipment.bridge$swapShearDrops(previousEquipment);
            }
        }
    }
}
