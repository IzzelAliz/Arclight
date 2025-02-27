package io.izzel.arclight.gradle.runnable;

import lombok.Getter;
import lombok.Setter;
import org.cadixdev.lorenz.MappingSet;

import java.nio.file.Path;
import java.util.Objects;

public class SourceRemapper implements Runnable {

    @Getter
    @Setter
    private Path sourceDir;

    @Getter
    @Setter
    private Path outputDir;

    @Getter
    @Setter
    private MappingSet mappings;

    @Override
    public void run() {
        Objects.requireNonNull(sourceDir);
        Objects.requireNonNull(outputDir);
        Objects.requireNonNull(mappings);


    }
}
