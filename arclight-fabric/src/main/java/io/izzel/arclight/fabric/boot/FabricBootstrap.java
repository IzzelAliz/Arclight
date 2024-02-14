package io.izzel.arclight.fabric.boot;

import io.izzel.arclight.boot.AbstractBootstrap;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;

import java.util.function.Consumer;

public class FabricBootstrap implements Consumer<FabricLauncher>, AbstractBootstrap {

    @Override
    public void accept(FabricLauncher launcher) {
        try {
            this.dirtyHacks();
            var provider = FabricLoaderImpl.INSTANCE.getGameProvider();
            var field = MinecraftGameProvider.class.getDeclaredField("transformer");
            field.setAccessible(true);
            var old = (GameTransformer) field.get(provider);
            field.set(provider, new ArclightImplementer(old, launcher));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}