package io.izzel.arclight.common.mod.util.remapper;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import io.izzel.arclight.api.PluginPatcher;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.i18n.ArclightConfig;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product2;
import io.izzel.tools.product.Product4;
import net.minecraftforge.fml.ModList;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.JarFile;

public abstract class ArclightClassCache implements AutoCloseable {

    public abstract CacheSegment makeSegment(URLConnection connection) throws IOException;

    public abstract void save() throws IOException;

    public interface CacheSegment {

        Optional<byte[]> findByName(String name) throws IOException;

        void addToCache(String name, byte[] value);

        void save() throws IOException;
    }

    private static final Marker MARKER = MarkerManager.getMarker("CLCACHE");
    private static final ArclightClassCache INSTANCE = new Impl();

    public static ArclightClassCache instance() {
        return INSTANCE;
    }

    private static class Impl extends ArclightClassCache {

        private final boolean enabled = ArclightConfig.spec().getOptimization().isCachePluginClass();
        private final ConcurrentHashMap<String, JarSegment> map = new ConcurrentHashMap<>();
        private final Path basePath = Paths.get(".arclight/class_cache");
        private ScheduledExecutorService executor;

        private static String currentVersionInfo() {
            var builder = new StringBuilder();
            var arclight = ModList.get().getModContainerById("arclight")
                .orElseThrow(IllegalStateException::new).getModInfo().getVersion().toString();
            builder.append(arclight);
            for (PluginPatcher patcher : ArclightRemapper.INSTANCE.getPatchers()) {
                builder.append('\0')
                    .append(patcher.getClass().getName())
                    .append('\0')
                    .append(patcher.version());
            }
            return builder.toString();
        }

        public Impl() {
            if (!enabled) return;
            executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r);
                thread.setName("arclight class cache saving thread");
                thread.setDaemon(true);
                return thread;
            });
            executor.scheduleWithFixedDelay(() -> {
                try {
                    this.save();
                } catch (IOException e) {
                    ArclightMod.LOGGER.error(MARKER, "Failed to save class cache", e);
                }
            }, 1, 10, TimeUnit.MINUTES);
            try {
                if (Files.isRegularFile(basePath)) {
                    Files.delete(basePath);
                }
                if (!Files.isDirectory(basePath)) {
                    Files.createDirectories(basePath);
                }
                String current = currentVersionInfo();
                String store;
                Path version = basePath.resolve(".version");
                if (Files.exists(version)) {
                    store = Files.readString(version);
                } else {
                    store = null;
                }
                boolean obsolete = !Objects.equals(current, store);
                Path index = basePath.resolve("index");
                if (obsolete) {
                    FileUtils.deleteDirectory(index.toFile());
                }
                if (!Files.exists(index)) {
                    Files.createDirectories(index);
                }
                Path blob = basePath.resolve("blob");
                if (obsolete) {
                    FileUtils.deleteDirectory(blob.toFile());
                }
                if (!Files.exists(blob)) {
                    Files.createDirectories(blob);
                }
                if (obsolete) {
                    Files.writeString(version, current, StandardOpenOption.CREATE);
                    ArclightMod.LOGGER.info(MARKER, "Obsolete plugin class cache is cleared");
                }
            } catch (IOException e) {
                ArclightMod.LOGGER.error(MARKER, "Failed to initialize class cache", e);
            }
            Thread thread = new Thread(() -> {
                try {
                    this.close();
                } catch (Exception e) {
                    ArclightMod.LOGGER.error(MARKER, "Failed to close class cache", e);
                }
            }, "arclight class cache cleanup");
            thread.setDaemon(true);
            Runtime.getRuntime().addShutdownHook(thread);
        }

        @Override
        public CacheSegment makeSegment(URLConnection connection) throws IOException {
            if (enabled && connection instanceof JarURLConnection) {
                JarFile file = ((JarURLConnection) connection).getJarFile();
                return this.map.computeIfAbsent(file.getName(), LamdbaExceptionUtils.rethrowFunction(JarSegment::new));
            } else {
                return new EmptySegment();
            }
        }

        @Override
        public void save() throws IOException {
            if (enabled) {
                for (CacheSegment segment : map.values()) {
                    segment.save();
                }
            }
        }

        @Override
        public void close() throws Exception {
            if (enabled) {
                save();
                executor.shutdownNow();
            }
        }

        private class JarSegment implements CacheSegment {

            private final Map<String, Product2<Long, Integer>> rangeMap = new ConcurrentHashMap<>();
            private final ConcurrentLinkedQueue<Product4<String, byte[], Long, Integer>> savingQueue = new ConcurrentLinkedQueue<>();
            private final AtomicLong sizeAllocator;
            private final Path indexPath, blobPath;

            private JarSegment(String fileName) throws IOException {
                Path jarFile = new File(fileName).toPath();
                Hasher hasher = Hashing.sha256().newHasher();
                hasher.putBytes(Files.readAllBytes(jarFile));
                String hash = hasher.hash().toString();
                this.indexPath = basePath.resolve("index").resolve(hash);
                this.blobPath = basePath.resolve("blob").resolve(hash);
                if (!Files.exists(indexPath)) {
                    Files.createFile(indexPath);
                }
                if (!Files.exists(blobPath)) {
                    Files.createFile(blobPath);
                }
                sizeAllocator = new AtomicLong(Files.size(blobPath));
                read();
            }

            @Override
            public Optional<byte[]> findByName(String name) throws IOException {
                Product2<Long, Integer> product2 = rangeMap.get(name);
                if (product2 != null) {
                    long off = product2._1;
                    int len = product2._2;
                    try (SeekableByteChannel channel = Files.newByteChannel(blobPath)) {
                        channel.position(off);
                        ByteBuffer buffer = ByteBuffer.allocate(len);
                        channel.read(buffer);
                        return Optional.of(buffer.array());
                    }
                } else {
                    return Optional.empty();
                }
            }

            @Override
            public void addToCache(String name, byte[] value) {
                int len = value.length;
                long off = sizeAllocator.getAndAdd(len);
                savingQueue.add(Product.of(name, value, off, len));
            }

            @Override
            public synchronized void save() throws IOException {
                if (savingQueue.isEmpty()) return;
                List<Product4<String, byte[], Long, Integer>> list = new ArrayList<>();
                while (!savingQueue.isEmpty()) {
                    list.add(savingQueue.poll());
                }
                try (OutputStream outIndex = Files.newOutputStream(indexPath, StandardOpenOption.APPEND);
                     DataOutputStream dataOutIndex = new DataOutputStream(outIndex);
                     SeekableByteChannel channel = Files.newByteChannel(blobPath, StandardOpenOption.WRITE)) {
                    for (Product4<String, byte[], Long, Integer> product4 : list) {
                        channel.position(product4._3);
                        channel.write(ByteBuffer.wrap(product4._2));
                        dataOutIndex.writeUTF(product4._1);
                        dataOutIndex.writeLong(product4._3);
                        dataOutIndex.writeInt(product4._4);
                        rangeMap.put(product4._1, Product.of(product4._3, product4._4));
                    }
                }
            }

            private synchronized void read() throws IOException {
                try (InputStream inputStream = Files.newInputStream(indexPath);
                     DataInputStream dataIn = new DataInputStream(inputStream)) {
                    while (dataIn.available() > 0) {
                        String name = dataIn.readUTF();
                        long off = dataIn.readLong();
                        int len = dataIn.readInt();
                        rangeMap.put(name, Product.of(off, len));
                    }
                }
            }
        }

        private static class EmptySegment implements CacheSegment {

            @Override
            public Optional<byte[]> findByName(String name) {
                return Optional.empty();
            }

            @Override
            public void addToCache(String name, byte[] value) {
            }

            @Override
            public void save() {
            }
        }
    }
}
