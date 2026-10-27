package io.izzel.arclight.common.mixin.core.server.packs.repository;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.common.bridge.core.server.packs.repository.PackRepositoryBridge;
import io.izzel.arclight.common.mod.server.pack.PaperDatapackDiscovery;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin implements PackRepositoryBridge {

    @Shadow public abstract void reload();

    @Shadow public abstract void setSelected(Collection<String> packs);

    @Unique private DirectoryValidator arclight$datapackValidator;
    @Unique private Set<Pack> arclight$automaticallyEnabledPacks = Collections.emptySet();
    @Unique private boolean arclight$pendingReload;

    @Override
    public void bridge$setDatapackValidator(DirectoryValidator validator) {
        Objects.requireNonNull(validator, "validator");
        if (this.arclight$datapackValidator != null) {
            throw new IllegalStateException("The datapack repository validator is already bound");
        }
        this.arclight$datapackValidator = validator;
    }

    @ModifyArg(method = "discoverAvailable()Ljava/util/Map;", at = @At(value = "INVOKE",
        target = "Lcom/google/common/collect/ImmutableMap;copyOf(Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;", remap = false),
        index = 0, require = 1, expect = 1, allow = 1)
    private Map<String, Pack> arclight$discoverPluginPacks(Map<String, Pack> discovered) {
        if (this.arclight$datapackValidator == null) {
            // Client/resource repositories have no server datapack validator.
            return discovered;
        }
        // Every native/mod source has succeeded; only this successful transaction
        // reaches the immutable snapshot subsequently adopted by native reload().
        PaperDatapackDiscovery.Result result = PaperDatapackDiscovery.discover(this.arclight$datapackValidator, discovered);
        this.arclight$automaticallyEnabledPacks = result.automaticallyEnabled();
        return result.packs();
    }

    @Decorate(method = "rebuildSelected(Ljava/util/Collection;)Ljava/util/List;", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/packs/repository/Pack;isRequired()Z"), require = 1, expect = 1, allow = 1)
    private boolean arclight$selectAutomaticPacks(Pack pack) throws Throwable {
        // Preserve every native/mod required pack and NeoForge's selection logic.
        // Only plugin packs whose configurer requested auto-enable are conditional.
        return (boolean) DecorationOps.callsite().invoke(pack) && (!this.arclight$automaticallyEnabledPacks.contains(pack) || this.arclight$pendingReload);
    }

    @Override
    public void bridge$reload(boolean pendingReload) {
        boolean previous = this.arclight$pendingReload;
        this.arclight$pendingReload = pendingReload;
        try {
            this.reload();
        } finally {
            this.arclight$pendingReload = previous;
        }
    }

    @Override
    public void bridge$setSelected(Collection<String> packs, boolean pendingReload) {
        boolean previous = this.arclight$pendingReload;
        this.arclight$pendingReload = pendingReload;
        try {
            this.setSelected(packs);
        } finally {
            this.arclight$pendingReload = previous;
        }
    }
}
