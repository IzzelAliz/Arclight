package io.izzel.arclight.common.mixin.core.world.level.storage.loot;

import io.izzel.arclight.common.bridge.core.world.level.storage.loot.LootTableBridge;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContextSource;
import net.minecraft.world.level.storage.loot.LootTable;
import org.bukkit.craftbukkit.CraftLootTable;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LootDataType.class)
public class LootDataTypeMixin {

    @Inject(method = "runValidation(Lnet/minecraft/world/level/storage/loot/ValidationContextSource;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/storage/loot/Validatable;)V", at = @At("RETURN"))
    private void arclight$setHandle(ValidationContextSource contextSource, ResourceKey<?> key, Validatable value, CallbackInfo ci) {
        if ((Object) this == LootDataType.TABLE && value instanceof LootTable table) {
            ((LootTableBridge) table).bridge$setCraftLootTable(new CraftLootTable(CraftNamespacedKey.fromMinecraft(key.identifier()), table));
        }
    }
}
