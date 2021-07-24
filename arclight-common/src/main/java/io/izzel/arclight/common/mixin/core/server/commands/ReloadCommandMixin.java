package io.izzel.arclight.common.mixin.core.server.commands;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.ReloadCommand;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;

@Mixin(ReloadCommand.class)
public abstract class ReloadCommandMixin {

    // @formatter:off
    @Shadow private static Collection<String> discoverNewPacks(PackRepository p_241058_0_, WorldData p_241058_1_, Collection<String> p_241058_2_) { return null; }
    // @formatter:on

    private static void reload(MinecraftServer minecraftserver) {
        PackRepository resourcePackList = minecraftserver.getPackRepository();
        WorldData configuration = minecraftserver.getWorldData();
        Collection<String> collection = resourcePackList.getSelectedIds();
        Collection<String> collection2 = discoverNewPacks(resourcePackList, configuration, collection);
        minecraftserver.reloadResources(collection2);
    }
}
