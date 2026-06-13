/*
 * ModLauncher - for launching Java programs with in-flight transformation ability.
 * Copyright (C) 2017-2019 cpw
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package net.neoforged.fml.classloading.transformation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

/**
 * Transforms classes using a {@link ClassProcessorSet} of the available {@link ClassProcessor}s.
 */
@ApiStatus.Internal
public class ClassTransformer {
    private static final byte[] EMPTY = getSha256().digest(new byte[0]);
    private static final Logger LOGGER = LogManager.getLogger();
    private final Marker CLASSDUMP = MarkerManager.getMarker("CLASSDUMP");
    private final ClassProcessorSet processors;
    private final ClassProcessorAuditLog auditTrail;

    public ClassTransformer(ClassProcessorSet processors, ClassProcessorAuditLog auditTrail) {
        this.processors = processors;
        this.auditTrail = auditTrail;
    }

    public byte[] transform(byte[] inputClass, String className, ProcessorName upToTransformer, ClassHierarchyRecomputationContext locator) {
        String internalName = className.replace('.', '/');
        Type classDesc = Type.getObjectType(internalName);

        ClassTransformStatistics.incrementLoadedClasses();

        var transformersToUse = this.processors.transformersFor(classDesc, inputClass.length == 0, upToTransformer);
        if (transformersToUse.isEmpty()) {
            return inputClass;
        }

        ClassTransformStatistics.incrementTransformedClasses();

        Supplier<byte[]> digest;
        ClassNode clazz = new ClassNode(Opcodes.ASM9);
        boolean isEmpty = inputClass.length == 0;
        if (inputClass.length > 0) {
            ClassReader classReader = new ClassReader(inputClass);
            classReader.accept(clazz, ClassReader.EXPAND_FRAMES);
            digest = () -> getSha256().digest(inputClass);
        } else {
            clazz.name = classDesc.getInternalName();
            clazz.version = Opcodes.V1_8;
            clazz.superName = Type.getInternalName(Object.class);
            digest = () -> EMPTY;
        }

        boolean allowsComputeFrames = false;

        var flags = ClassProcessor.ComputeFlags.NO_REWRITE;
        for (var transformer : transformersToUse) {
            if (ClassProcessorIds.COMPUTING_FRAMES.equals(transformer.name())) {
                allowsComputeFrames = true;
                continue;
            }
            var trail = auditTrail.forClassProcessor(classDesc.getClassName(), transformer);
            var context = new ClassProcessor.TransformationContext(
                    classDesc,
                    clazz,
                    isEmpty,
                    trail,
                    digest);
            var newFlags = transformer.processClass(context);
            if (newFlags != ClassProcessor.ComputeFlags.NO_REWRITE) {
                trail.rewrites();
                isEmpty = false;
            }
            flags = flags.max(newFlags);
            if (flags.ordinal() >= ClassProcessor.ComputeFlags.COMPUTE_FRAMES.ordinal()) {
                if (!processors.canRecomputeFrames(transformer.name())) {
                    LOGGER.error("Transformer {} requested COMPUTE_FRAMES but does not depend, directly or indirectly, on running after {}", transformer.name(), ClassProcessorIds.COMPUTING_FRAMES);
                    throw new IllegalStateException("Transformer " + transformer.name() + " requested COMPUTE_FRAMES but does not depend, directly or indirectly, on running after " + ClassProcessorIds.COMPUTING_FRAMES);
                }
                if (!allowsComputeFrames) {
                    LOGGER.error("Transformer {} requested COMPUTE_FRAMES but is not allowed to do so as it runs before transformer {}", transformer.name(), ClassProcessorIds.COMPUTING_FRAMES);
                    throw new IllegalStateException("Transformer " + transformer.name() + " requested COMPUTE_FRAMES but is not allowed to do so as it runs before transformer " + ClassProcessorIds.COMPUTING_FRAMES);
                }
            }
        }
        if (upToTransformer == null) {
            // run post-result callbacks
            var context = new ClassProcessor.AfterProcessingContext(classDesc);
            for (var transformer : transformersToUse) {
                transformer.afterProcessing(context);
            }
        }

        if (flags == ClassProcessor.ComputeFlags.NO_REWRITE) {
            return inputClass; // No changes were made, return the original class
        }

        ClassWriter cw = createClassWriter(flags, clazz, locator);
        clazz.accept(cw);
        // if upToTransformer is null, we are doing this for classloading purposes
        if (LOGGER.isEnabled(Level.TRACE) && upToTransformer == null && LOGGER.isEnabled(Level.TRACE, CLASSDUMP)) {
            dumpClass(cw.toByteArray(), className);
        }
        return cw.toByteArray();
    }

    private static volatile Path tempDir;

    private void dumpClass(byte[] clazz, String className) {
        if (tempDir == null) {
            synchronized (ClassTransformer.class) {
                if (tempDir == null) {
                    try {
                        tempDir = Files.createTempDirectory("classDump");
                    } catch (IOException e) {
                        LOGGER.error("Failed to create temporary directory");
                        return;
                    }
                }
            }
        }
        try {
            Path tempFile = tempDir.resolve(className + ".class");
            Files.write(tempFile, clazz);
            LOGGER.info("Wrote {} byte class file {} to {}", clazz.length, className, tempFile);
        } catch (IOException e) {
            LOGGER.error("Failed to write class file {}", className, e);
        }
    }

    private static MessageDigest getSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("HUH");
        }
    }

    private static ClassWriter createClassWriter(ClassProcessor.ComputeFlags flags, ClassNode clazzAccessor, ClassHierarchyRecomputationContext locator) {
        int writerFlag = switch (flags) {
            case COMPUTE_MAXS -> ClassWriter.COMPUTE_MAXS;
            case COMPUTE_FRAMES -> ClassWriter.COMPUTE_FRAMES;
            default -> 0;
        };

        //Only use the TransformerClassWriter when needed as it's slower, and only COMPUTE_FRAMES calls getCommonSuperClass
        return flags.ordinal() >= ClassProcessor.ComputeFlags.COMPUTE_FRAMES.ordinal() ? new TransformerClassWriter(writerFlag, clazzAccessor, locator) : new ClassWriter(writerFlag);
    }
}
