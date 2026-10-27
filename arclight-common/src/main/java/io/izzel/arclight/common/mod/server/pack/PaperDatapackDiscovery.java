package io.izzel.arclight.common.mod.server.pack;

import io.papermc.paper.datapack.DatapackRegistrar;
import io.papermc.paper.datapack.DiscoveredDatapack;
import io.papermc.paper.datapack.PaperDatapackRegistrar;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventRunner;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

/** Connects Paper's registrar to the native repository discovery transaction. */
public final class PaperDatapackDiscovery {

    private PaperDatapackDiscovery() {
    }

    public static Result discover(DirectoryValidator validator, Map<String, Pack> discovered) {
        Map<String, Pack> packs = new UniquePackMap(discovered);
        Set<Pack> automaticallyEnabled = Collections.newSetFromMap(new IdentityHashMap<>());
        PaperDatapackRegistrar registrar = new Registrar(validator, packs, automaticallyEnabled);
        try {
            LifecycleEventRunner.INSTANCE.callStaticRegistrarEvent(LifecycleEvents.DATAPACK_DISCOVERY,
                registrar, BootstrapContext.class);
        } finally {
            // A retained registrar must not gain a valid owner after dispatch, even on failure.
            registrar.setCurrentContext(null);
        }
        // Keep native/mod pack identity and ordering, including NeoForge's child packs.
        automaticallyEnabled.removeIf(pack -> packs.get(pack.getId()) != pack);
        return new Result(packs, Collections.unmodifiableSet(automaticallyEnabled));
    }

    public record Result(Map<String, Pack> packs, Set<Pack> automaticallyEnabled) {
    }

    private static final class Registrar extends PaperDatapackRegistrar {

        private final Set<Pack> automaticallyEnabled;

        private Registrar(DirectoryValidator validator, Map<String, Pack> discovered,
                          Set<Pack> automaticallyEnabled) {
            super(validator, discovered);
            this.automaticallyEnabled = automaticallyEnabled;
        }

        @Override
        public @Nullable DiscoveredDatapack discoverPack(PluginMeta pluginMeta, Path path, String id,
                Consumer<DatapackRegistrar.Configurer> configurer) throws IOException {
            String packId = Objects.requireNonNull(pluginMeta, "pluginMeta").getName() + "/"
                + Objects.requireNonNull(id, "id");
            if (this.discoveredPacks.containsKey(packId)) {
                throw duplicateId(packId);
            }
            // Retain Paper's safe URI conversion, native directory/symlink validator,
            // resources supplier, metadata parsing, title and position configuration.
            DiscoveredDatapack pack = super.discoverPack(pluginMeta, path, id, configurer);
            if (pack != null) {
                Pack nativePack = this.discoveredPacks.get(packId);
                if (nativePack.isRequired()) {
                    // This bit means autoEnableOnServerStart for plugin packs, not
                    // permanent native required status. The repository gates it by
                    // its explicit pending-reload selection scope.
                    this.automaticallyEnabled.add(nativePack);
                }
            }
            return pack;
        }
    }

    private static IllegalArgumentException duplicateId(String id) {
        return new IllegalArgumentException("A datapack with id '" + id + "' has already been discovered");
    }

    private static final class UniquePackMap extends AbstractMap<String, Pack> {

        private final Map<String, Pack> entries;

        private UniquePackMap(Map<String, Pack> discovered) {
            // Vanilla sorts globally; NeoForge preserves source/child ordering.
            this.entries = discovered instanceof SortedMap<?, ?> ? new TreeMap<>(discovered) : new LinkedHashMap<>(discovered);
        }

        @Override
        public Set<Entry<String, Pack>> entrySet() {
            return this.entries.entrySet();
        }

        @Override
        public Pack get(Object id) {
            return this.entries.get(id);
        }

        @Override
        public boolean containsKey(Object id) {
            return this.entries.containsKey(id);
        }

        @Override
        public Pack remove(Object id) {
            return this.entries.remove(id);
        }

        @Override
        public Pack put(String id, Pack pack) {
            // Recheck at insertion: a configurer can discover another pack reentrantly.
            if (this.containsKey(id)) {
                throw duplicateId(id);
            }
            return this.entries.put(id, pack);
        }
    }
}
