package io.izzel.arclight.mod;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ArclightConfig {

    public static ArclightConfig INSTANCE;

    public Optimization optimizations;

    static void init(Path path) {
        ArclightConnector.LOGGER.info("Loading configurations from {}", path);
        try {
            if (!Files.exists(path)) {
                InputStream stream = ArclightConfig.class.getResourceAsStream("/arclight.yml");
                Files.copy(stream, path);
            }
        } catch (IOException e) {
            ArclightConnector.LOGGER.error("Failed to save default configurations", e);
        }
        try {
            Yaml yaml = new Yaml(new Constructor(ArclightConfig.class));
            Object load = yaml.load(Files.newInputStream(path));
            INSTANCE = (ArclightConfig) load;
        } catch (IOException e) {
            ArclightConnector.LOGGER.error("Failed to load configurations", e);
        }
    }

    public static class Optimization {

        public boolean removeStreams = true;

    }
}
