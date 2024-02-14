package io.izzel.arclight.installer;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Supplier;

public record FileDownloader(String url, String target, String hash) implements Supplier<Path> {

    @Override
    public Path get() {
        try {
            Path path = new File(target).toPath();
            if (Files.exists(path) && Files.isDirectory(path)) {
                Files.delete(path);
            }
            if (Files.exists(path)) {
                if (Files.isDirectory(path)) {
                    throw new FileAlreadyExistsException(target);
                } else {
                    if (Util.hash(path).equalsIgnoreCase(hash)) return path;
                    else Files.delete(path);
                }
            }
            if (!Files.exists(path) && path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            var tmp = new File(target + ".tmp").toPath();
            try (InputStream stream = read(url)) {
                Files.copy(stream, tmp, StandardCopyOption.REPLACE_EXISTING);
            } catch (SocketTimeoutException | SSLException e) {
                throw new RuntimeException("Timeout " + url);
            }
            if (Files.exists(tmp)) {
                String hash = Util.hash(tmp);
                if (hash.equalsIgnoreCase(this.hash)) {
                    Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
                    return path;
                } else {
                    Files.delete(tmp);
                    throw new RuntimeException("Hash not match, expect %s found %s in %s".formatted(this.hash, hash, url));
                }
            } else {
                throw new RuntimeException("Not found " + url);
            }
        } catch (AccessDeniedException e) {
            throw new RuntimeException("Access denied for file " + e.getFile(), e);
        } catch (Exception e) {
            Util.throwException(e);
            return null;
        }
    }

    static InputStream read(String url) throws IOException {
        return redirect(new URL(url));
    }

    private static InputStream redirect(URL url) throws IOException {
        return redirect(url, new HashSet<>());
    }

    private static InputStream redirect(URL url, Set<String> history) throws IOException {
        if (history.contains(url.toString())) {
            StringJoiner joiner = new StringJoiner("\n        ");
            joiner.add("");
            history.forEach(joiner::add);
            throw new RuntimeException("Redirect error " + joiner);
        } else {
            history.add(url.toString());
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setReadTimeout(15000);
        connection.setConnectTimeout(15000);
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return connection.getInputStream();
        } else if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            String location = URLDecoder.decode(connection.getHeaderField("Location"), StandardCharsets.UTF_8);
            return redirect(new URL(url, location));
        } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
            throw new RuntimeException("Not found " + url);
        } else {
            throw new RemoteException("Http " + responseCode + " " + url);
        }
    }
}
