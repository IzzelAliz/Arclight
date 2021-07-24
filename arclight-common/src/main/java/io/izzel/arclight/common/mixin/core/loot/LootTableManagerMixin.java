package io.izzel.arclight.common.mixin.core.loot;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;

@Mixin(LootTables.class)
public class LootTableManagerMixin {

    // @formatter:off
    @Shadow private Map<ResourceLocation, LootTable> tables;
    // @formatter:on

    public Map<LootTable, ResourceLocation> lootTableToKey = ImmutableMap.of();

    @Inject(method = "apply", at = @At("RETURN"))
    private void arclight$buildRev(Map<ResourceLocation, JsonObject> objectIn, ResourceManager resourceManagerIn, ProfilerFiller profilerIn, CallbackInfo ci) {
        ImmutableMap.Builder<LootTable, ResourceLocation> lootTableToKeyBuilder = ImmutableMap.builder();
        this.tables.forEach((lootTable, key) -> lootTableToKeyBuilder.put(key, lootTable));
        this.lootTableToKey = lootTableToKeyBuilder.build();
    }
}
