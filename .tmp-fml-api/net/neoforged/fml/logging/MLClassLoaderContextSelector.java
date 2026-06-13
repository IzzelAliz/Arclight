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

import net.neoforged.fml.classloading.ModuleClassLoader;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;

/**
 * A custom context selector to avoid initializing multiple log4j contexts due to {@link ModuleClassLoader#getParent()} always returning null (as a {@link ModuleClassLoader} can have multiple parents).
 * As all {@link ModuleClassLoader}s should get the same log4j context, we just return a static string with "MCL", otherwise we use the default logic
 */
public class MLClassLoaderContextSelector extends ClassLoaderContextSelector {
    @Override
    protected String toContextMapKey(ClassLoader loader) {
        if (loader instanceof ModuleClassLoader) {
            return "MCL";
        }
        return super.toContextMapKey(loader);
    }
}
