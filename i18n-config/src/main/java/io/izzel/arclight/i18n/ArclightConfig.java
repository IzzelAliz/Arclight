package io.izzel.arclight.i18n;

import com.google.common.reflect.TypeToken;
import io.izzel.arclight.i18n.conf.ConfigSpec;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.StringJoiner;

public class ArclightConfig {

    private static ArclightConfig instance;

    private final CommentedConfigurationNode node;
    private final ConfigSpec spec;

    public ArclightConfig(CommentedConfigurationNode node) throws ObjectMappingException {
        this.node = node;
        this.spec = this.node.getValue(TypeToken.of(ConfigSpec.class));
    }

    public CommentedConfigurationNode getNode() {
        return node;
    }

    public ConfigSpec getSpec() {
        return spec;
    }

    public ConfigurationNode get(String path) {
        return this.node.getNode((Object[]) path.split("\\."));
    }

    public static ConfigSpec spec() {
        return instance.spec;
    }

    private static void load() throws Exception {
        Path path = Paths.get("arclight.conf");
        CommentedConfigurationNode node = HoconConfigurationLoader.builder().setSource(
            () -> new BufferedReader(new InputStreamReader(ArclightConfig.class.getResourceAsStream("/META-INF/arclight.conf"), StandardCharsets.UTF_8))
        ).build().load();
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder().setPath(path).build();
        CommentedConfigurationNode cur = loader.load();
        cur.mergeValuesFrom(node);
        cur.getNode("locale", "current").setValue(ArclightLocale.getInstance().getCurrent());
        fillComments(cur, ArclightLocale.getInstance());
        instance = new ArclightConfig(cur);
        loader.save(cur);
    }

    private static void fillComments(CommentedConfigurationNode node, ArclightLocale locale) {
        if (!node.getComment().isPresent()) {
            String path = pathOf(node);
            Optional<String> option = locale.getOption("comments." + path + ".comment");
            option.ifPresent(node::setComment);
        }
        if (node.hasMapChildren()) {
            for (CommentedConfigurationNode value : node.getChildrenMap().values()) {
                fillComments(value, locale);
            }
        }
    }

    private static String pathOf(ConfigurationNode node) {
        StringJoiner joiner = new StringJoiner(".");
        for (Object o : node.getPath()) {
            if (o != null) {
                joiner.add(o.toString());
            }
        }
        String s = joiner.toString();
        return s.isEmpty() ? "__root__" : s;
    }

    static {
        try {
            load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
