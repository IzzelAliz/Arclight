package io.izzel.arclight.common.mixin.core.command.impl;

import net.minecraft.command.impl.ReloadCommand;
import net.minecraft.resources.ResourcePackList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.IServerConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;

@Mixin(ReloadCommand.class)
public abstract class ReloadCommandMixin {

    // @formatter:off
    @Shadow private static Collection<String> func_241058_a_(ResourcePackList p_241058_0_, IServerConfiguration p_241058_1_, Collection<String> p_241058_2_) { return null; }
    // @formatter:on

    private static void reload(MinecraftServer minecraftserver) {
        ResourcePackList resourcePackList = minecraftserver.getResourcePacks();
        IServerConfiguration configuration = minecraftserver.getServerConfiguration();
        Collection<String> collection = resourcePackList.func_232621_d_();
        Collection<String> collection2 = func_241058_a_(resourcePackList, configuration, collection);
        minecraftserver.func_240780_a_(collection2);
    }
}
