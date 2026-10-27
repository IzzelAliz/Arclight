package io.izzel.arclight.gradle.processor;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.loom.api.processor.MinecraftJarProcessor;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public record StaticMethodInjectionSpec(int formatVersion, List<Method> methods) implements MinecraftJarProcessor.Spec {
    private static final Pattern INTERNAL_NAME = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(/[A-Za-z_$][A-Za-z0-9_$]*)*");
    private static final Pattern METHOD_NAME = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");

    public StaticMethodInjectionSpec {
        methods = List.copyOf(methods);
    }

    public record Method(String owner, String name, String descriptor, String mixin, String signature) {
        public String location() {
            return owner + "." + name + descriptor;
        }
    }

    public static StaticMethodInjectionSpec read(Path file) throws IOException {
        try (var reader = Files.newBufferedReader(file)) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            var methods = new ArrayList<Method>();
            Set<String> seen = new HashSet<>();
            if (root.has("injected_static_methods")) {
                for (var entry : root.getAsJsonObject("injected_static_methods").entrySet()) {
                    String owner = internalName(entry.getKey());
                    for (var element : entry.getValue().getAsJsonArray()) {
                        var method = element.getAsJsonObject();
                        if (!method.keySet().equals(Set.of("name", "descriptor", "mixin"))
                            && !method.keySet().equals(Set.of("name", "descriptor", "mixin", "signature"))) {
                            throw new IllegalArgumentException("Expected name, descriptor, mixin for " + owner);
                        }
                        String name = string(method.get("name"));
                        String descriptor = string(method.get("descriptor"));
                        String mixin = internalName(string(method.get("mixin")));
                        if (!METHOD_NAME.matcher(name).matches() || !validDescriptor(descriptor)) {
                            throw new IllegalArgumentException("Invalid static method: " + owner + "." + name + descriptor);
                        }
                        if (!seen.add(owner + "." + name + descriptor)) {
                            throw new IllegalArgumentException("Duplicate static method: " + owner + "." + name + descriptor);
                        }
                        methods.add(new Method(owner, name, descriptor, mixin, method.has("signature") ? string(method.get("signature")) : null));
                    }
                }
            }
            methods.sort(Comparator.comparing(Method::owner).thenComparing(Method::name).thenComparing(Method::descriptor));
            return new StaticMethodInjectionSpec(2, methods);
        } catch (RuntimeException e) {
            throw new IOException("Invalid static method injection specification " + file + ": " + e.getMessage(), e);
        }
    }

    private static boolean validDescriptor(String descriptor) {
        if (!descriptor.startsWith("(")) return false;
        int end = descriptor.indexOf(')');
        if (end < 1) return false;
        String field = "\\[{0,255}(?:[ZBCSIJFD]|L[A-Za-z_$][A-Za-z0-9_$]*(?:/[A-Za-z_$][A-Za-z0-9_$]*)*;)";
        if (!descriptor.substring(1, end).matches("(?:" + field + ")*")
            || !descriptor.substring(end + 1).matches("(?:V|" + field + ")")) return false;
        int slots = 0;
        for (var argument : Type.getArgumentTypes(descriptor)) slots += argument.getSize();
        return slots <= 255;
    }

    private static String internalName(String value) {
        if (!INTERNAL_NAME.matcher(value).matches()) throw new IllegalArgumentException("Invalid JVM internal name: " + value);
        return value;
    }

    private static String string(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Expected JSON string: " + value);
        }
        return value.getAsString();
    }
}
