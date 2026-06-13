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

package net.neoforged.fml.logging;

import net.neoforged.fml.classloading.transformation.ClassProcessorAuditSource;
import net.neoforged.fml.loading.FMLLoader;
import org.apache.logging.log4j.core.pattern.TextRenderer;
import org.jetbrains.annotations.Nullable;

public class ExtraDataTextRenderer implements TextRenderer {
    private final TextRenderer wrapped;
    @Nullable
    private final ClassProcessorAuditSource auditLog;
    private final ThreadLocal<TransformerContext> currentClass = new ThreadLocal<>();

    ExtraDataTextRenderer(TextRenderer wrapped) {
        this.wrapped = wrapped;
        var loader = FMLLoader.getCurrentOrNull();
        this.auditLog = loader != null ? loader.getClassTransformerAuditLog() : null;
    }

    @Override
    public void render(String input, StringBuilder output, String styleName) {
        if ("StackTraceElement.ClassName".equals(styleName)) {
            currentClass.set(new TransformerContext());
            currentClass.get().setClassName(input);
        } else if ("StackTraceElement.MethodName".equals(styleName)) {
            TransformerContext transformerContext = currentClass.get();
            if (transformerContext != null) {
                transformerContext.setMethodName(input);
            }
        } else if ("Suffix".equals(styleName)) {
            TransformerContext classContext = currentClass.get();
            currentClass.remove();
            if (classContext != null) {
                var auditLine = auditLog != null ? auditLog.getAuditString(classContext.getClassName()) : null;
                wrapped.render(" {" + auditLine + "}", output, "StackTraceElement.Transformers");
            }
            return;
        }
        wrapped.render(input, output, styleName);
    }

    @Override
    public void render(StringBuilder input, StringBuilder output) {
        wrapped.render(input, output);
    }

    private static class TransformerContext {
        private String className;
        private String methodName;

        public void setClassName(String className) {
            this.className = className;
        }

        public String getClassName() {
            return className;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public String getMethodName() {
            return methodName;
        }

        @Override
        public String toString() {
            return getClassName() + "." + getMethodName();
        }
    }
}
