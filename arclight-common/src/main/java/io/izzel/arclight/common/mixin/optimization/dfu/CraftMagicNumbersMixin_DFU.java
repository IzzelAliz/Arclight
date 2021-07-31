package io.izzel.arclight.common.mixin.optimization.dfu;

import io.izzel.arclight.i18n.ArclightLocale;
import org.bukkit.craftbukkit.v.util.CraftMagicNumbers;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftMagicNumbers.class, remap = false)
public class CraftMagicNumbersMixin_DFU {

    @Inject(method = "checkSupported", at = @At("HEAD"))
    private void arclight$dfuDisabled(PluginDescriptionFile pdf, CallbackInfo ci) throws InvalidPluginException {
        if (pdf.getAPIVersion() == null) {
            throw new InvalidPluginException(ArclightLocale.getInstance().get("dfu-disable.legacy-plugin"));
        }
    }
}
