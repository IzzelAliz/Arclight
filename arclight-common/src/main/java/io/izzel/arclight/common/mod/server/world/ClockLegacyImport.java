package io.izzel.arclight.common.mod.server.world;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** File boundary for importing one clock snapshot before world SavedDataStorage caches it. */
public final class ClockLegacyImport {
    private ClockLegacyImport() {}

    public static boolean regularFileExists(Path file) throws IOException {
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Clock saved data is not a regular file: " + file);
        }
        return true;
    }

    @FunctionalInterface
    public interface Writer {
        void write(Path temporary) throws Exception;
    }

    public static boolean publishIfAbsent(Path target, Writer writer) throws Exception {
        if (regularFileExists(target)) {
            return false;
        }
        Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), ".world_clocks-", ".tmp");
        try {
            writer.write(temp);
            try {
                Files.createLink(target, temp);
                return true;
            } catch (FileAlreadyExistsException existing) {
                // Target was produced concurrently; its contents take precedence.
                regularFileExists(target);
                return false;
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
