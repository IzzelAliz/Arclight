package io.izzel.arclight.common.mixin.core.loot;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTableManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(LootTableManager.class)
public class LootTableManagerMixin {

    // @formatter:off
    @Shadow private Map<ResourceLocation, LootTable> registeredLootTables;
    // @formatter:on

    public Map<LootTable, ResourceLocation> lootTableToKey = ImmutableMap.of();

    @Inject(method = "apply", at = @At("RETURN"))
    private void arclight$buildRev(Map<ResourceLocation, JsonObject> objectIn, IResourceManager resourceManagerIn, IProfiler profilerIn, CallbackInfo ci) {
        ImmutableMap.Builder<LootTable, ResourceLocation> lootTableToKeyBuilder = ImmutableMap.builder();
        this.registeredLootTables.forEach((lootTable, key) -> lootTableToKeyBuilder.put(key, lootTable));
        this.lootTableToKey = lootTableToKeyBuilder.build();
    }
}
