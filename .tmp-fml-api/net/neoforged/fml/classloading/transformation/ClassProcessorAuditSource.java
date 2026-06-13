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

import net.neoforged.neoforgespi.transformation.ClassProcessor;

/**
 * A source for getting audit information related to the activity of
 * {@linkplain ClassProcessor class processors}.
 */
public interface ClassProcessorAuditSource {
    /**
     * Retrieve a formatted string summarizing actions for the supplied class
     *
     * @param clazz The class
     * @return A formatted string
     */
    String getAuditString(String clazz);
}
