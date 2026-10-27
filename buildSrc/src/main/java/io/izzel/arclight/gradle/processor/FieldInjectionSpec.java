package io.izzel.arclight.gradle.processor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import net.fabricmc.loom.api.processor.MinecraftJarProcessor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public record FieldInjectionSpec(int formatVersion, List<Field> fields) implements MinecraftJarProcessor.Spec {
    private static final int FORMAT_VERSION = 5;
    private static final Pattern INTERNAL_NAME = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(/[A-Za-z_$][A-Za-z0-9_$]*)*");
    private static final Pattern FIELD_NAME = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
    private static final Pattern DESCRIPTOR = Pattern.compile("\\[{0,255}(?:[ZBCSIJFD]|L[A-Za-z_$][A-Za-z0-9_$]*(?:/[A-Za-z_$][A-Za-z0-9_$]*)*;)");
    public static final int ACCESS_MASK = Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED
        | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_VOLATILE | Opcodes.ACC_TRANSIENT;

    public FieldInjectionSpec {
        fields = List.copyOf(fields);
    }

    public record Field(String owner, String name, String descriptor, int access, String mixin, String signature) {
        public String location() {
            return owner + "." + name + ":" + descriptor;
        }
    }

    public static FieldInjectionSpec read(Path file) throws IOException {
        try (Reader input = Files.newBufferedReader(file); JsonReader reader = new JsonReader(input)) {
            reader.setLenient(false);
            JsonObject root = readObject(reader).getAsJsonObject();
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new IllegalArgumentException("Trailing JSON content");
            }
            List<Field> fields = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            if (root.has("injected_fields")) {
                for (var entry : root.getAsJsonObject("injected_fields").entrySet()) {
                    String owner = internalName(entry.getKey());
                    for (JsonElement element : entry.getValue().getAsJsonArray()) {
                        JsonObject field = element.getAsJsonObject();
                        if (!field.keySet().equals(Set.of("name", "descriptor", "access", "mixin"))
                            && !field.keySet().equals(Set.of("name", "descriptor", "access", "mixin", "signature"))) {
                            throw new IllegalArgumentException("Expected name, descriptor, access, mixin for " + owner);
                        }
                        String name = string(field.get("name"));
                        String descriptor = string(field.get("descriptor"));
                        String mixin = internalName(string(field.get("mixin")));
                        if (!FIELD_NAME.matcher(name).matches() || !DESCRIPTOR.matcher(descriptor).matches()) {
                            throw new IllegalArgumentException("Invalid field: " + owner + "." + name + ":" + descriptor);
                        }
                        if (!seen.add(owner + "." + name)) {
                            throw new IllegalArgumentException("Duplicate field: " + owner + "." + name);
                        }
                        int access = 0;
                        for (JsonElement flag : field.getAsJsonArray("access")) {
                            int bit = switch (string(flag)) {
                                case "public" -> Opcodes.ACC_PUBLIC;
                                case "final" -> Opcodes.ACC_FINAL;
                                case "volatile" -> Opcodes.ACC_VOLATILE;
                                case "transient" -> Opcodes.ACC_TRANSIENT;
                                default -> throw new IllegalArgumentException("Unsupported field access: " + flag);
                            };
                            if ((access & bit) != 0) throw new IllegalArgumentException("Duplicate access: " + flag);
                            access |= bit;
                        }
                        if ((access & Opcodes.ACC_PUBLIC) == 0) {
                            throw new IllegalArgumentException("Injected field must be public: " + owner + "." + name);
                        }
                        fields.add(new Field(owner, name, descriptor, access, mixin,
                            field.has("signature") ? string(field.get("signature")) : null));
                    }
                }
            }
            fields.sort(Comparator.comparing(Field::owner).thenComparing(Field::name));
            return new FieldInjectionSpec(FORMAT_VERSION, fields);
        } catch (RuntimeException e) {
            throw new IOException("Invalid field injection specification " + file + ": " + e.getMessage(), e);
        }
    }

    private static String string(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Expected JSON string: " + element);
        }
        return element.getAsString();
    }

    private static String internalName(String value) {
        if (!INTERNAL_NAME.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid JVM internal name: " + value);
        }
        return value;
    }

    private static JsonElement readObject(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.BEGIN_OBJECT) {
            JsonObject object = new JsonObject();
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (object.has(key)) throw new IOException("Duplicate JSON key: " + key);
                object.add(key, readObject(reader));
            }
            reader.endObject();
            return object;
        }
        if (reader.peek() == JsonToken.BEGIN_ARRAY) {
            var array = new com.google.gson.JsonArray();
            reader.beginArray();
            while (reader.hasNext()) array.add(readObject(reader));
            reader.endArray();
            return array;
        }
        return switch (reader.peek()) {
            case STRING -> new com.google.gson.JsonPrimitive(reader.nextString());
            case BOOLEAN -> new com.google.gson.JsonPrimitive(reader.nextBoolean());
            case NUMBER -> JsonParser.parseString(reader.nextString());
            case NULL -> { reader.nextNull(); yield com.google.gson.JsonNull.INSTANCE; }
            default -> throw new IOException("Unexpected JSON token: " + reader.peek());
        };
    }
}
