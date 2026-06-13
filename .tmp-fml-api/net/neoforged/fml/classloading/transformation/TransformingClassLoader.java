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

import java.lang.module.Configuration;
import java.util.List;
import net.neoforged.fml.classloading.ModuleClassLoader;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Module transforming class loader
 */
@ApiStatus.Internal
public class TransformingClassLoader extends ModuleClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }
    private final ClassTransformer classTransformer;

    @VisibleForTesting
    public TransformingClassLoader(ClassProcessorSet classProcessorSet, ClassProcessorAuditLog auditTrail, Configuration configuration, List<ModuleLayer> parentLayers, ClassLoader parentClassLoader) {
        super("TRANSFORMER", configuration, parentLayers, parentClassLoader);
        this.classTransformer = new ClassTransformer(classProcessorSet, auditTrail);
        // The state of this class has to be set up fully before the processors are linked
        classProcessorSet.link(processorName -> className -> buildTransformedClassNodeFor(className, processorName));
    }

    @Override
    protected byte[] maybeTransformClassBytes(byte[] bytes, String name, @Nullable String upToTransformer) {
        var upToTransformerName = upToTransformer == null ? null : ProcessorName.parse(upToTransformer);
        return classTransformer.transform(bytes, name, upToTransformerName, new ClassHierarchyRecomputationContext() {
            @Override
            public @Nullable Class<?> findLoadedClass(String name) {
                return TransformingClassLoader.this.getLoadedClass(name);
            }

            @Override
            public byte[] upToFrames(String className) throws ClassNotFoundException {
                return TransformingClassLoader.this.buildTransformedClassNodeFor(className, ClassProcessorIds.COMPUTING_FRAMES);
            }

            @Override
            public Class<?> locateParentClass(String className) throws ClassNotFoundException {
                return Class.forName(className, false, TransformingClassLoader.this.getParent());
            }
        });
    }

    private Class<?> getLoadedClass(String name) {
        return findLoadedClass(name);
    }

    byte[] buildTransformedClassNodeFor(String className, ProcessorName upToTransformer) throws ClassNotFoundException {
        return super.getMaybeTransformedClassBytes(className, upToTransformer.toString());
    }
}
