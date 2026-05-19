package io.izzel.arclight.installer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

final class FabricApiPatcher {

    private static final String FABRIC_API = "net.fabricmc.fabric-api:fabric-api:";
    private static final String EVENTS_INTERACTION = "META-INF/jars/fabric-events-interaction-v0-4.1.1+3b89ecf63e.jar";
    private static final String SERVER_PLAYER_GAME_MODE_MIXIN = "net/fabricmc/fabric/mixin/event/interaction/ServerPlayerGameModeMixin.class";

    private FabricApiPatcher() {
    }

    static void patchFabricApi(InstallInfo info) throws IOException {
        for (String coord : info.fabricDeps().keySet()) {
            if (coord.startsWith(FABRIC_API)) {
                patchFabricApiJar(Path.of("libraries", Util.mavenToPath(coord)));
            }
        }
    }

    private static void patchFabricApiJar(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        byte[] original = Files.readAllBytes(path);
        byte[] patched = rewriteZip(original, EVENTS_INTERACTION, module -> rewriteZip(module, SERVER_PLAYER_GAME_MODE_MIXIN, FabricApiPatcher::patchMixinClass));
        if (patched != original) {
            Files.write(path, patched);
        }
    }

    private static byte[] rewriteZip(byte[] input, String target, ThrowingFunction<byte[], byte[]> patcher) throws IOException {
        boolean changed = false;
        ByteArrayOutputStream output = new ByteArrayOutputStream(input.length);
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(input));
             ZipOutputStream zout = new ZipOutputStream(output)) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                byte[] bytes = zin.readAllBytes();
                if (entry.getName().equals(target)) {
                    byte[] next = patcher.apply(bytes);
                    if (next != bytes) {
                        bytes = next;
                        changed = true;
                    }
                }
                ZipEntry out = new ZipEntry(entry.getName());
                out.setTime(entry.getTime());
                zout.putNextEntry(out);
                zout.write(bytes);
                zout.closeEntry();
            }
        }
        return changed ? output.toByteArray() : input;
    }

    private static byte[] patchMixinClass(byte[] input) {
        ClassFile file = new ClassFile(input);
        int indexName = file.ensureUtf8("index");
        int intFour = file.ensureInteger(4);
        boolean changed = file.patchOnBlockBrokenLocalIndex(indexName, intFour);
        return changed ? file.toByteArray() : input;
    }

    @FunctionalInterface
    private interface ThrowingFunction<T, R> {
        R apply(T value) throws IOException;
    }

    private static final class ClassFile {
        private byte[] data;
        private final List<Entry> cp = new ArrayList<>();
        private int cpEnd;

        ClassFile(byte[] data) {
            this.data = data.clone();
            parseConstantPool();
        }

        int ensureUtf8(String value) {
            for (int i = 1; i < cp.size(); i++) {
                Entry entry = cp.get(i);
                if (entry != null && entry.tag == 1 && value.equals(entry.value)) {
                    return i;
                }
            }
            int index = cp.size();
            byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] extra = new byte[3 + bytes.length];
            extra[0] = 1;
            writeU2(extra, 1, bytes.length);
            System.arraycopy(bytes, 0, extra, 3, bytes.length);
            insertConstant(index, extra, new Entry(1, value));
            return index;
        }

        int ensureInteger(int value) {
            for (int i = 1; i < cp.size(); i++) {
                Entry entry = cp.get(i);
                if (entry != null && entry.tag == 3 && Integer.valueOf(value).equals(entry.value)) {
                    return i;
                }
            }
            int index = cp.size();
            byte[] extra = new byte[5];
            extra[0] = 3;
            writeU4(extra, 1, value);
            insertConstant(index, extra, new Entry(3, value));
            return index;
        }

        boolean patchOnBlockBrokenLocalIndex(int indexName, int intFour) {
            int p = cpEnd + 6;
            int interfaces = u2(data, p);
            p += 2 + 2 * interfaces;
            int fields = u2(data, p);
            p += 2;
            for (int i = 0; i < fields; i++) {
                p = skipMember(p);
            }
            int methods = u2(data, p);
            p += 2;
            boolean changed = false;
            for (int i = 0; i < methods; i++) {
                int methodStart = p;
                int nameIndex = u2(data, p + 2);
                String name = utf(nameIndex);
                int attrCount = u2(data, p + 6);
                p += 8;
                for (int a = 0; a < attrCount; a++) {
                    int attrStart = p;
                    String attrName = utf(u2(data, p));
                    int len = u4(data, p + 2);
                    p += 6;
                    if ("onBlockBroken".equals(name) && "RuntimeInvisibleParameterAnnotations".equals(attrName)) {
                        byte[] oldInfo = slice(data, p, len);
                        byte[] newInfo = patchParameterAnnotations(oldInfo, indexName, intFour);
                        if (newInfo != oldInfo) {
                            data = replace(data, p, p + len, newInfo);
                            writeU4(data, attrStart + 2, newInfo.length);
                            int delta = newInfo.length - len;
                            p += newInfo.length;
                            changed = true;
                            continue;
                        }
                    }
                    p += len;
                }
                if (p < methodStart) {
                    throw new IllegalStateException();
                }
            }
            return changed;
        }

        private byte[] patchParameterAnnotations(byte[] info, int indexName, int intFour) {
            int p = 0;
            int params = info[p++] & 0xFF;
            ByteArrayOutputStream out = new ByteArrayOutputStream(info.length + 8);
            out.write(params);
            boolean changed = false;
            for (int param = 0; param < params; param++) {
                int annotations = u2(info, p);
                p += 2;
                ByteArrayOutputStream paramOut = new ByteArrayOutputStream();
                for (int a = 0; a < annotations; a++) {
                    int type = u2(info, p);
                    int pairs = u2(info, p + 2);
                    p += 4;
                    String annotationType = utf(type);
                    ByteArrayOutputStream existing = new ByteArrayOutputStream();
                    for (int pair = 0; pair < pairs; pair++) {
                        existing.write(info, p, 2);
                        int tag = info[p + 2] & 0xFF;
                        existing.write(tag);
                        p += 3;
                        if ("BCDFIJSZs".indexOf(tag) >= 0 || tag == 'c') {
                            existing.write(info, p, 2);
                            p += 2;
                        } else if (tag == 'e') {
                            existing.write(info, p, 4);
                            p += 4;
                        } else {
                            throw new IllegalStateException("Unsupported annotation tag " + (char) tag);
                        }
                    }
                    if (param == 3 && "Lcom/llamalad7/mixinextras/sugar/Local;".equals(annotationType)) {
                        writeU2(paramOut, type);
                        writeU2(paramOut, 1);
                        writeU2(paramOut, indexName);
                        paramOut.write('I');
                        writeU2(paramOut, intFour);
                        changed = true;
                    } else {
                        writeU2(paramOut, type);
                        writeU2(paramOut, pairs);
                        paramOut.writeBytes(existing.toByteArray());
                    }
                }
                writeU2(out, annotations);
                out.writeBytes(paramOut.toByteArray());
            }
            return changed ? out.toByteArray() : info;
        }

        byte[] toByteArray() {
            return data;
        }

        private void insertConstant(int index, byte[] bytes, Entry entry) {
            int oldCount = u2(data, 8);
            data = replace(data, cpEnd, cpEnd, bytes);
            writeU2(data, 8, oldCount + 1);
            cp.add(entry);
            cpEnd += bytes.length;
        }

        private void parseConstantPool() {
            cp.clear();
            cp.add(null);
            int p = 8;
            int count = u2(data, p);
            p += 2;
            for (int i = 1; i < count; i++) {
                int tag = data[p++] & 0xFF;
                switch (tag) {
                    case 1 -> {
                        int len = u2(data, p);
                        p += 2;
                        cp.add(new Entry(tag, new String(data, p, len, java.nio.charset.StandardCharsets.UTF_8)));
                        p += len;
                    }
                    case 3 -> {
                        cp.add(new Entry(tag, u4(data, p)));
                        p += 4;
                    }
                    case 4 -> { cp.add(new Entry(tag, null)); p += 4; }
                    case 5, 6 -> { cp.add(new Entry(tag, null)); cp.add(null); p += 8; i++; }
                    case 7, 8, 16, 19, 20 -> { cp.add(new Entry(tag, null)); p += 2; }
                    case 9, 10, 11, 12, 17, 18 -> { cp.add(new Entry(tag, null)); p += 4; }
                    case 15 -> { cp.add(new Entry(tag, null)); p += 3; }
                    default -> throw new IllegalStateException("Unknown constant tag " + tag);
                }
            }
            cpEnd = p;
        }

        private int skipMember(int p) {
            p += 6;
            int attrs = u2(data, p);
            p += 2;
            for (int i = 0; i < attrs; i++) {
                int len = u4(data, p + 2);
                p += 6 + len;
            }
            return p;
        }

        private String utf(int index) {
            Entry entry = cp.get(index);
            return entry != null && entry.tag == 1 ? (String) entry.value : null;
        }

        private static byte[] slice(byte[] data, int start, int len) {
            byte[] out = new byte[len];
            System.arraycopy(data, start, out, 0, len);
            return out;
        }

        private static byte[] replace(byte[] data, int start, int end, byte[] replacement) {
            byte[] out = new byte[data.length - (end - start) + replacement.length];
            System.arraycopy(data, 0, out, 0, start);
            System.arraycopy(replacement, 0, out, start, replacement.length);
            System.arraycopy(data, end, out, start + replacement.length, data.length - end);
            return out;
        }

        private static int u2(byte[] data, int p) {
            return ((data[p] & 0xFF) << 8) | (data[p + 1] & 0xFF);
        }

        private static int u4(byte[] data, int p) {
            return ((data[p] & 0xFF) << 24) | ((data[p + 1] & 0xFF) << 16) | ((data[p + 2] & 0xFF) << 8) | (data[p + 3] & 0xFF);
        }

        private static void writeU2(ByteArrayOutputStream out, int value) {
            out.write((value >>> 8) & 0xFF);
            out.write(value & 0xFF);
        }

        private static void writeU2(byte[] data, int p, int value) {
            data[p] = (byte) (value >>> 8);
            data[p + 1] = (byte) value;
        }

        private static void writeU4(byte[] data, int p, int value) {
            data[p] = (byte) (value >>> 24);
            data[p + 1] = (byte) (value >>> 16);
            data[p + 2] = (byte) (value >>> 8);
            data[p + 3] = (byte) value;
        }
    }

    private record Entry(int tag, Object value) {
    }
}
