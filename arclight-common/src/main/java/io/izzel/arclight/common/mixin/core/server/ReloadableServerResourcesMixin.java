package io.izzel.arclight.common.mixin.core.server;

import io.izzel.arclight.common.bridge.core.server.ReloadableServerResourcesBridge;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ReloadableServerResources.class)
public abstract class ReloadableServerResourcesMixin implements ReloadableServerResourcesBridge {

    @Unique private CommandBuildContext arclight$commandBuildContext;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$rememberCommandBuildContext(ReloadableServerRegistries.LoadResult loadingContext,
            FeatureFlagSet enabledFeatures,
            Commands.CommandSelection commandSelection, List<Registry.PendingTags<?>> postponedTags,
            PermissionSet functionCompilationPermissions,
            List<DataComponentInitializers.PendingComponents<?>> newComponents, CallbackInfo ci) {
        // A completed constructor is still only a reload candidate. Keep its lookup
        // with the candidate, without publishing it through Paper's live registrar.
        this.arclight$commandBuildContext = CommandBuildContext.simple(loadingContext.lookupWithUpdatedTags(), enabledFeatures);
    }

    @Override
    public CommandBuildContext bridge$getCommandBuildContext() {
        return this.arclight$commandBuildContext;
    }
}
