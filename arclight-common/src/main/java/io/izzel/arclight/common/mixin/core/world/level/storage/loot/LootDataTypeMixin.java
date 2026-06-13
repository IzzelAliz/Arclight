package io.izzel.arclight.common.mixin.core.world.level.storage.loot;

import io.izzel.arclight.common.bridge.core.world.level.storage.loot.LootTableBridge;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContextSource;
import org.bukkit.craftbukkit.CraftLootTable;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LootDataType.class)
public class LootDataTypeMixin<T extends net.minecraft.world.level.storage.loot.Validatable> {

    @Inject(method = "runValidation", at = @At("RETURN"))
    private void arclight$setHandle(ValidationContextSource contextSource, ResourceKey<T> resourceKey, T value, CallbackInfo ci) {
        if (value instanceof LootTable lootTable) {
            ((LootTableBridge) lootTable).bridge$setCraftLootTable(
                new CraftLootTable(CraftNamespacedKey.fromMinecraft(resourceKey.identifier()), lootTable));
        }
    }
}
