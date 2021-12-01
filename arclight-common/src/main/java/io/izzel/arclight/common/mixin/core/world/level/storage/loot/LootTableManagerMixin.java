package io.izzel.arclight.common.mixin.core.world.level.storage.loot;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(LootTables.class)
public class LootTableManagerMixin {

    // @formatter:off
    @Shadow private Map<ResourceLocation, LootTable> tables;
    // @formatter:on

    public Map<LootTable, ResourceLocation> lootTableToKey = ImmutableMap.of();

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("RETURN"))
    private void arclight$buildRev(Map<ResourceLocation, JsonObject> objectIn, ResourceManager resourceManagerIn, ProfilerFiller profilerIn, CallbackInfo ci) {
        Map<LootTable, ResourceLocation> lootTableToKeyBuilder = new HashMap<>();
        this.tables.forEach((lootTable, key) -> lootTableToKeyBuilder.put(key, lootTable));
        this.lootTableToKey = ImmutableMap.copyOf(lootTableToKeyBuilder);
    }
}
