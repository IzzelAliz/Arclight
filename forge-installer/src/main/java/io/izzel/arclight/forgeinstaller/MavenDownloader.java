package io.izzel.arclight.forgeinstaller;

import io.izzel.arclight.i18n.ArclightLocale;
import io.izzel.arclight.i18n.LocalizedException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Supplier;

public class MavenDownloader implements Supplier<Path> {

    private static final Function<String, String> FORGE_TO_BMCLAPI =
        s -> s.replace("https://files.minecraftforge.net/maven/", "https://download.mcbbs.net/maven/");

    private final LinkedList<String> urls;
    private final String coord;
    private final String target;
    private final String hash;

    public MavenDownloader(String[] repos, String coord, String target, String hash) {
        this.urls = new LinkedList<>();
        this.coord = coord;
        this.target = target;
        this.hash = hash;
        String path = Util.mavenToPath(coord);
        for (String repo : repos) {
            this.urls.add(repo + path);
        }
    }

    public MavenDownloader(String[] repos, String coord, String target, String hash, String sourceUrl) {
        this(repos, coord, target, hash);
        if (sourceUrl != null && !this.urls.contains(sourceUrl)) {
            this.urls.addFirst(sourceUrl);
            this.urls.addFirst(FORGE_TO_BMCLAPI.apply(sourceUrl));
        }
    }

    @Override
    public Path get() {
        List<Exception> exceptions = new ArrayList<>();
        for (String url : this.urls) {
            try {
                return new FileDownloader(url, target, hash).get();
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        StringJoiner joiner = new StringJoiner("\n  ");
        joiner.add("");
        for (int i = 0; i < exceptions.size(); i++) {
            Exception exception = exceptions.get(i);
            if (exception instanceof LocalizedException) {
                LocalizedException local = (LocalizedException) exception;
                String format = ArclightLocale.getInstance().format(local.node(), local.args());
                joiner.add("(" + (i + 1) + ") " + format);
            } else {
                joiner.add("(" + (i + 1) + ") " + exception);
            }
        }
        throw LocalizedException.unchecked("downloader.maven-fail", coord, joiner.toString());
    }
}
