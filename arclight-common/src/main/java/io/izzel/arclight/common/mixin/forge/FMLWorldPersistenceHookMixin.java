package io.izzel.arclight.common.mixin.forge;

import com.google.common.collect.Multimap;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.IServerConfiguration;
import net.minecraft.world.storage.SaveFormat;
import net.minecraftforge.fml.FMLWorldPersistenceHook;
import net.minecraftforge.fml.MavenVersionStringHelper;
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

@Mixin(value = FMLWorldPersistenceHook.class, remap = false)
public class FMLWorldPersistenceHookMixin {

    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private static Marker WORLDPERSISTENCE;

    private final Map<Path, CompoundNBT> map = new HashMap<>();
    private boolean injected = false;

    @Redirect(method = "readData", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/registries/GameData;injectSnapshot(Ljava/util/Map;ZZ)Lcom/google/common/collect/Multimap;"))
    private Multimap<ResourceLocation, ResourceLocation> arclight$handleInject(Map<ResourceLocation, ForgeRegistry.Snapshot> snapshot, boolean injectFrozenData, boolean isLocalWorld,
                                                                               SaveFormat.LevelSave levelSave, IServerConfiguration serverInfo, CompoundNBT tag) {
        if (!injected) {
            injected = true;
            return GameData.injectSnapshot(snapshot, injectFrozenData, isLocalWorld);
        } else {
            // TODO Properly remap registry and id
            map.put(levelSave.getWorldDir(), tag.getCompound("Registries").copy());
            LOGGER.debug(WORLDPERSISTENCE, "Skipped registry injection for {}", serverInfo.getWorldName());
            return null;
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public CompoundNBT getDataForWriting(SaveFormat.LevelSave levelSave, IServerConfiguration serverInfo) {
        CompoundNBT fmlData = new CompoundNBT();
        ListNBT modList = new ListNBT();
        ModList.get().getMods().forEach(mi ->
        {
            final CompoundNBT mod = new CompoundNBT();
            mod.putString("ModId", mi.getModId());
            mod.putString("ModVersion", MavenVersionStringHelper.artifactVersionToString(mi.getVersion()));
            modList.add(mod);
        });
        fmlData.put("LoadingModList", modList);

        CompoundNBT nbt = map.get(levelSave.getWorldDir());
        if (nbt != null) {
            fmlData.put("Registries", nbt);
            LOGGER.debug(WORLDPERSISTENCE, "Skipped ID Map collection for {}", serverInfo.getWorldName());
        } else {
            CompoundNBT registries = new CompoundNBT();
            fmlData.put("Registries", registries);
            LOGGER.debug(WORLDPERSISTENCE, "Gathering id map for writing to world save {}", serverInfo.getWorldName());

            for (Map.Entry<ResourceLocation, ForgeRegistry.Snapshot> e : RegistryManager.ACTIVE.takeSnapshot(true).entrySet()) {
                registries.put(e.getKey().toString(), e.getValue().write());
            }
            LOGGER.debug(WORLDPERSISTENCE, "ID Map collection complete {}", serverInfo.getWorldName());
        }
        return fmlData;
    }
}
