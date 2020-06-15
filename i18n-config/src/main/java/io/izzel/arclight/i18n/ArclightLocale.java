package io.izzel.arclight.i18n;

import ninja.leaping.configurate.ValueType;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.Callable;

public class ArclightLocale {

    private static ArclightLocale instance;

    private final String current, fallback;
    private final CommentedConfigurationNode node;

    public ArclightLocale(String current, String fallback, CommentedConfigurationNode node) {
        this.current = current;
        this.fallback = fallback;
        this.node = node;
    }

    public String getCurrent() {
        return current;
    }

    public String getFallback() {
        return fallback;
    }

    public CommentedConfigurationNode getNode() {
        return node;
    }

    public String format(String node, Object... args) {
        return MessageFormat.format(get(node), args);
    }

    public String get(String path) {
        return getOption(path).orElse(path);
    }

    public Optional<String> getOption(String path) {
        CommentedConfigurationNode node = this.node.getNode((Object[]) path.split("\\."));
        if (node.getValueType() == ValueType.LIST) {
            StringJoiner joiner = new StringJoiner("\n");
            for (CommentedConfigurationNode configurationNode : node.getChildrenList()) {
                joiner.add(configurationNode.getString());
            }
            return Optional.ofNullable(joiner.toString());
        } else {
            return Optional.ofNullable(node.getString());
        }
    }

    public static void info(String path, Object... args) {
        System.out.println(instance.format(path, args));
    }

    public static void error(String path, Object... args) {
        System.err.println(instance.format(path, args));
    }

    public static ArclightLocale getInstance() {
        return instance;
    }

    private static void init() throws Exception {
        Map.Entry<String, String> entry = getLocale();
        String current = entry.getKey();
        String fallback = entry.getValue();
        InputStream stream = ArclightLocale.class.getResourceAsStream("/META-INF/i18n/" + fallback + ".conf");
        if (stream == null) throw new RuntimeException("Fallback locale is not found: " + fallback);
        CommentedConfigurationNode node = HoconConfigurationLoader.builder().setSource(localeSource(fallback)).build().load();
        instance = new ArclightLocale(current, fallback, node);
        if (!current.equals(fallback)) {
            try {
                CommentedConfigurationNode curNode = HoconConfigurationLoader.builder().setSource(localeSource(current)).build().load();
                curNode.mergeValuesFrom(node);
                instance = new ArclightLocale(current, fallback, curNode);
            } catch (Exception e) {
                System.err.println(instance.format("i18n.current-not-available", current));
            }
        }
    }

    private static Callable<BufferedReader> localeSource(String path) {
        return () -> new BufferedReader(new InputStreamReader(ArclightLocale.class.getResourceAsStream("/META-INF/i18n/" + path + ".conf"), StandardCharsets.UTF_8));
    }

    private static Map.Entry<String, String> getLocale() {
        try {
            Path path = Paths.get("arclight.conf");
            if (!Files.exists(path)) {
                throw new Exception();
            } else {
                CommentedConfigurationNode node = HoconConfigurationLoader.builder().setPath(path).build().load();
                CommentedConfigurationNode locale = node.getNode("locale");
                String current = locale.getNode("current").getString(currentLocale());
                String fallback = locale.getNode("fallback").getString("zh_cn");
                return new AbstractMap.SimpleImmutableEntry<>(current, fallback);
            }
        } catch (Throwable t) {
            return new AbstractMap.SimpleImmutableEntry<>(currentLocale(), "zh_cn");
        }
    }

    private static String currentLocale() {
        Locale locale = Locale.getDefault();
        return locale.getLanguage().toLowerCase(Locale.ROOT) + "_" + locale.getCountry().toLowerCase(Locale.ROOT);
    }

    static {
        try {
            init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
