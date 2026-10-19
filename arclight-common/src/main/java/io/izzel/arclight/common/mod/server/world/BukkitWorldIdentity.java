package io.izzel.arclight.common.mod.server.world;

import io.izzel.arclight.common.bridge.core.world.level.storage.LevelStorageSourceBridge.LevelStorageAccessBridge;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;

/** Bukkit identity only: native dimension storage and session metadata keep their own paths. */
public final class BukkitWorldIdentity {

    private BukkitWorldIdentity() {
    }

    public static Path getWorldFolder(LevelStorageSource.LevelStorageAccess access, ResourceKey<Level> dimension) {
        return ((LevelStorageAccessBridge) access).bridge$getTypeKey() != null
            ? access.getLevelDirectory().path() : access.getDimensionPath(dimension);
    }

    /**
     * Preserve every applicable identity before writing a missing canonical file. An ambiguous
     * or unreadable save needs recovery by its owner, not a randomly substituted identity.
     * Session locks protect saves; synchronization also prevents in-process first-read races.
     */
    public static synchronized UUID getUUID(LevelStorageSource.LevelStorageAccess access, ResourceKey<Level> dimension) {
        Path canonical = getWorldFolder(access, dimension).resolve("uid.dat");
        var candidates = new LinkedHashSet<Path>();
        candidates.add(canonical);
        Path root = access.getLevelDirectory().path();
        var type = ((LevelStorageAccessBridge) access).bridge$getTypeKey();
        if (type != null) {
            // The previous published typed-session UUID used the actual stem, not the
            // Bukkit instance key. getDimensionPath retains that existing bridge policy.
            candidates.add(access.getDimensionPath(dimension).resolve("uid.dat"));
        }
        var id = type == null ? dimension.identifier() : type.identifier();
        if ("minecraft".equals(id.getNamespace())) {
            Path legacy = switch (id.getPath()) {
                case "overworld" -> root;
                case "the_nether" -> root.resolve("DIM-1");
                case "the_end" -> root.resolve("DIM1");
                default -> null;
            };
            if (legacy != null) candidates.add(legacy.resolve("uid.dat"));
        }

        Map<Path, UUID> identities = new LinkedHashMap<>();
        try {
            for (Path candidate : candidates) {
                UUID uid = readExisting(candidate);
                if (uid != null) identities.put(candidate, uid);
            }
            var values = new LinkedHashSet<>(identities.values());
            if (values.size() > 1) {
                throw new IllegalStateException("Conflicting Bukkit world UUIDs; explicit identity recovery required: " + identities);
            }
            UUID existing = identities.get(canonical);
            if (existing != null) return existing;
            UUID uid = values.isEmpty() ? UUID.randomUUID() : values.iterator().next();
            Files.createDirectories(canonical.getParent());
            try (var output = new DataOutputStream(Files.newOutputStream(canonical, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
                output.writeLong(uid.getMostSignificantBits());
                output.writeLong(uid.getLeastSignificantBits());
            } catch (FileAlreadyExistsException ex) {
                // Never overwrite a concurrently created identity or return our unused UUID.
                return getUUID(access, dimension);
            }
            // Return only after the complete file has been written and closed successfully.
            return uid;
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot resolve or persist Bukkit world UUID at " + canonical
                + "; identity candidates: " + candidates, ex);
        }
    }

    private static UUID readExisting(Path file) throws IOException {
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException ex) {
            return null;
        }
        // A validated, readable link still has an existing identity; preserve it without
        // rewriting either link or target. A dangling/unreadable link must not be absence.
        if (attributes.isSymbolicLink()) {
            attributes = Files.readAttributes(file, BasicFileAttributes.class);
        }
        if (!attributes.isRegularFile() || attributes.size() != 16) {
            throw new IOException("Invalid Bukkit UUID file (expected a regular 16-byte file): " + file);
        }
        try (var input = new DataInputStream(Files.newInputStream(file))) {
            UUID uid = new UUID(input.readLong(), input.readLong());
            if (input.read() != -1) throw new IOException("Invalid Bukkit UUID file length: " + file);
            return uid;
        }
    }
}
