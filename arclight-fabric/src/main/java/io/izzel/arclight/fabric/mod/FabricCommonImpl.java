package io.izzel.arclight.fabric.mod;

import io.izzel.arclight.common.mod.ArclightCommon;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.transformer.FabricTransformer;
import org.objectweb.asm.ClassReader;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

public class FabricCommonImpl implements ArclightCommon.Api {

    @Override
    public byte[] platformRemapClass(byte[] cl) {
        var name = new ClassReader(cl).getClassName();
        var bytes = FabricTransformer.transform(false, EnvType.SERVER, name.replace('/', '.'), cl);
        bytes = ((IMixinTransformer) MixinEnvironment.getCurrentEnvironment().getActiveTransformer()).transformClassBytes(name, name, bytes);
        return bytes;
    }

    @Override
    public boolean isModLoaded(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }
}