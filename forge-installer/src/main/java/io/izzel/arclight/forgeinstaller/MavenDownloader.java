package io.izzel.arclight.forgeinstaller;

import io.izzel.arclight.i18n.ArclightLocale;
import io.izzel.arclight.i18n.LocalizedException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Supplier;

public class MavenDownloader implements Supplier<Path> {

    private final String[] repos;
    private final String coord;
    private final String target;
    private final String hash;

    public MavenDownloader(String[] repos, String coord, String target, String hash) {
        this.repos = repos;
        this.coord = coord;
        this.target = target;
        this.hash = hash;
    }

    @Override
    public Path get() {
        String path = Util.mavenToPath(coord);
        List<Exception> exceptions = new ArrayList<>();
        for (String repo : repos) {
            try {
                return new FileDownloader(repo + path, target, hash).get();
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
