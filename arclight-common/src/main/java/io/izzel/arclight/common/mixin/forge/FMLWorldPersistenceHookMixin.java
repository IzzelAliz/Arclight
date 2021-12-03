package io.izzel.arclight.common.mixin.forge;

import com.google.common.collect.Multimap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.minecraftforge.common.util.MavenVersionStringHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.GameData;
import net.minecraftforge.registries.RegistryManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Mixin(targets = "net.minecraftforge.common.ForgeMod$FMLWorldPersistenceHook", remap = false)
public class FMLWorldPersistenceHookMixin {

    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private static Marker WORLDPERSISTENCE;

    private final Map<Path, CompoundTag> map = new HashMap<>();
    private boolean injected = false;

    @Redirect(method = "readData", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/registries/GameData;injectSnapshot(Ljava/util/Map;ZZ)Lcom/google/common/collect/Multimap;"))
    private Multimap<ResourceLocation, ResourceLocation> arclight$handleInject(Map<ResourceLocation, ForgeRegistry.Snapshot> snapshot, boolean injectFrozenData, boolean isLocalWorld,
                                                                               LevelStorageSource.LevelStorageAccess levelSave, WorldData serverInfo, CompoundTag tag) {
        if (!injected) {
            injected = true;
            return GameData.injectSnapshot(snapshot, injectFrozenData, isLocalWorld);
        } else {
            // TODO Properly remap registry and id
            map.put(levelSave.getWorldDir(), tag.getCompound("Registries").copy());
            LOGGER.debug(WORLDPERSISTENCE, "Skipped registry injection for {}", serverInfo.getLevelName());
            return null;
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public CompoundTag getDataForWriting(LevelStorageSource.LevelStorageAccess levelSave, WorldData serverInfo) {
        CompoundTag fmlData = new CompoundTag();
        ListTag modList = new ListTag();
        ModList.get().getMods().forEach(mi ->
        {
            final CompoundTag mod = new CompoundTag();
            mod.putString("ModId", mi.getModId());
            mod.putString("ModVersion", MavenVersionStringHelper.artifactVersionToString(mi.getVersion()));
            modList.add(mod);
        });
        fmlData.put("LoadingModList", modList);

        CompoundTag nbt = map.get(levelSave.getWorldDir());
        if (nbt != null) {
            fmlData.put("Registries", nbt);
            LOGGER.debug(WORLDPERSISTENCE, "Skipped ID Map collection for {}", serverInfo.getLevelName());
        } else {
            CompoundTag registries = new CompoundTag();
            fmlData.put("Registries", registries);
            LOGGER.debug(WORLDPERSISTENCE, "Gathering id map for writing to world save {}", serverInfo.getLevelName());

            for (Map.Entry<ResourceLocation, ForgeRegistry.Snapshot> e : RegistryManager.ACTIVE.takeSnapshot(true).entrySet()) {
                registries.put(e.getKey().toString(), e.getValue().write());
            }
            LOGGER.debug(WORLDPERSISTENCE, "ID Map collection complete {}", serverInfo.getLevelName());
        }
        return fmlData;
    }
}
