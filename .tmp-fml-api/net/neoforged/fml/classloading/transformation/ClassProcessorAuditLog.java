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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

@ApiStatus.Internal
public class ClassProcessorAuditLog implements ClassProcessorAuditSource {
    private final Map<String, List<TransformerActivity>> audit = new ConcurrentHashMap<>();

    @VisibleForTesting
    public void clear() {
        audit.clear();
    }

    static final class TransformerActivity implements BiConsumer<String, String[]> {
        private final ProcessorName processorName;
        private final List<String> activities = new ArrayList<>();
        private boolean include = false;

        private TransformerActivity(ProcessorName processorName) {
            this.processorName = processorName;
        }

        private boolean shouldInclude() {
            return include || !activities.isEmpty();
        }

        void rewrites() {
            include = true;
        }

        private String getActivityString() {
            return processorName + (activities.isEmpty() ? "" : "[" + String.join(",", activities) + "]");
        }

        @Override
        public void accept(String activity, String... context) {
            activities.add(activity + (context.length == 0 ? "" : ":" + String.join(":", context)));
        }
    }

    TransformerActivity forClassProcessor(String clazz, ClassProcessor classProcessor) {
        var activities = getTransformerActivities(clazz);
        var activity = new TransformerActivity(classProcessor.name());
        activities.add(activity);
        return activity;
    }

    private List<TransformerActivity> getTransformerActivities(String clazz) {
        return audit.computeIfAbsent(clazz, k -> new ArrayList<>());
    }

    @Override
    public String getAuditString(String clazz) {
        return audit.getOrDefault(clazz, Collections.emptyList()).stream()
                .filter(TransformerActivity::shouldInclude)
                .map(TransformerActivity::getActivityString).collect(Collectors.joining(","));
    }
}
