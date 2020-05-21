package io.izzel.arclight.forgeinstaller;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class SimpleDownloader {

    private static final String[] MAVEN_REPO = {
        "https://repo.spongepowered.org/maven",
        "https://oss.sonatype.org/content/repositories/snapshots/",
        "https://hub.spigotmc.org/nexus/content/repositories/snapshots/",
        "https://bmclapi2.bangbang93.com/maven/",
        "https://maven.aliyun.com/repository/public/"
    };

    private final CompletableFuture<Void> future = new CompletableFuture<>();
    private final AtomicInteger integer = new AtomicInteger(0);
    private final ExecutorService service = Executors.newFixedThreadPool(8);
    private final AtomicBoolean error = new AtomicBoolean(false);

    public void run(Callable<Boolean> callable) {
        integer.incrementAndGet();
        CompletableFuture.supplyAsync(() -> {
            try {
                return callable.call();
            } catch (Throwable e) {
                System.err.println(e.toString());
                error.compareAndSet(false, true);
                return false;
            }
        }, service).thenAccept(b -> {
            int remain = integer.decrementAndGet();
            if (remain == 0) {
                if (error.get())
                    future.completeExceptionally(new Exception());
                else
                    future.complete(null);
            }
        });
    }

    public void download(String url, String dist, String hash) {
        download(url, dist, hash, path -> {});
    }

    public void download(String url, String dist, String hash, Consumer<Path> onComplete) {
        run(() -> downloadFile(url, dist, hash, 5, onComplete));
    }

    public void downloadMaven(String path) {
        run(() -> {
            for (String s : MAVEN_REPO) {
                if (downloadFile(s + path, "libraries/" + path, null, 3, p -> {})) {
                    return true;
                }
            }
            return false;
        });
    }

    public boolean awaitTermination() {
        try {
            future.join();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            service.shutdownNow();
        }
    }

    private boolean downloadFile(String url, String dist, String hash, int retry, Consumer<Path> onComplete) throws Exception {
        if (retry <= 0) return false;
        try {
            Path path = Paths.get(dist);
            if (Files.exists(path) && hash != null) {
                String hash1 = hash(path);
                if (hash.equals(hash1)) {
                    onComplete.accept(path);
                    return true;
                } else {
                    Files.delete(path);
                    throw new Exception("Checksum failed: expect " + hash + " but found " + hash1);
                }
            }
            if (!Files.exists(path) && path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            System.out.println("Downloading " + url);
            InputStream stream = redirect(new URL(url));
            Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING);
            stream.close();
            if (hash != null) {
                String hash1 = hash(path);
                if (!hash.equals(hash1)) {
                    Files.delete(path);
                    throw new Exception("Checksum failed: expect " + hash + " but found " + hash1);
                }
            }
            onComplete.accept(path);
            return true;
        } catch (FileNotFoundException | SocketTimeoutException e) {
            return false;
        } catch (Exception e) {
            run(() -> downloadFile(url, dist, hash, retry - 1, onComplete));
            System.err.println("Failed to download file " + dist);
            throw e;
        }
    }

    private static InputStream redirect(URL url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setReadTimeout(15000);
        connection.setConnectTimeout(15000);
        switch (connection.getResponseCode()) {
            case HttpURLConnection.HTTP_MOVED_PERM:
            case HttpURLConnection.HTTP_MOVED_TEMP:
                String location = URLDecoder.decode(connection.getHeaderField("Location"), "UTF-8");
                return redirect(new URL(url, location));
            case HttpURLConnection.HTTP_FORBIDDEN:
            case HttpURLConnection.HTTP_NOT_FOUND:
                throw new FileNotFoundException();
        }
        return connection.getInputStream();
    }

    private static String SHA_PAD = String.format("%040d", 0);

    private static String hash(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        String hash = new BigInteger(1, digest.digest(Files.readAllBytes(path))).toString(16);
        return (SHA_PAD + hash).substring(hash.length());
    }
}
