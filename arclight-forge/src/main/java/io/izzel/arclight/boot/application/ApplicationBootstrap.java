package io.izzel.arclight.boot.application;

import io.izzel.arclight.api.EnumHelper;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.boot.AbstractBootstrap;
import io.izzel.arclight.i18n.ArclightConfig;
import io.izzel.arclight.i18n.ArclightLocale;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ServiceLoader;
import java.util.function.Consumer;

public class ApplicationBootstrap extends AbstractBootstrap implements Consumer<String[]> {

    private static final int MIN_DEPRECATED_VERSION = 60;
    private static final int MIN_DEPRECATED_JAVA_VERSION = 16;

    @Override
    @SuppressWarnings("unchecked")
    public void accept(String[] args) {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        System.setProperty("log4j.jul.LoggerAdapter", "io.izzel.arclight.boot.log.ArclightLoggerAdapter");
        System.setProperty("log4j.configurationFile", "arclight-log4j2.xml");
        ArclightLocale.info("i18n.using-language", ArclightConfig.spec().getLocale().getCurrent(), ArclightConfig.spec().getLocale().getFallback());
        try {
            int javaVersion = (int) Float.parseFloat(System.getProperty("java.class.version"));
            if (javaVersion < MIN_DEPRECATED_VERSION) {
                ArclightLocale.error("java.deprecated", System.getProperty("java.version"), MIN_DEPRECATED_JAVA_VERSION);
                Thread.sleep(3000);
            }
            Unsafe.ensureClassInitialized(EnumHelper.class);
        } catch (Throwable t) {
            System.err.println("Your Java is not compatible with Arclight.");
            t.printStackTrace();
            return;
        }
        try {
            this.setupMod();
            this.dirtyHacks();
            this.hackModlauncher();
            ServiceLoader.load(getClass().getModule().getLayer(), Consumer.class).stream()
                .filter(it -> !it.type().getName().contains("arclight"))
                .findFirst().orElseThrow().get().accept(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fail to launch Arclight.");
        }
    }

    private void hackModlauncher() throws Exception {
        try (var in = getClass().getClassLoader().getResourceAsStream("cpw/mods/modlauncher/TransformerClassWriter$SuperCollectingVisitor.class")) {
            var cw = new ClassWriter(0);
            var cr = new ClassReader(in);
            cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    var mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    if (name.equals("<init>")) {
                        return new MethodVisitor(Opcodes.ASM9, mv) {
                            @Override
                            public void visitLdcInsn(Object value) {
                                if (value.equals(Opcodes.ASM7)) {
                                    super.visitLdcInsn(Opcodes.ASM9);
                                } else {
                                    super.visitLdcInsn(value);
                                }
                            }
                        };
                    } else return mv;
                }
            }, 0);
            var bytes = cw.toByteArray();
            Unsafe.defineClass(cr.getClassName(), bytes, 0, bytes.length, getClass().getClassLoader(), getClass().getProtectionDomain());
        }
    }
}
