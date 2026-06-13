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

package net.neoforged.neoforgespi.transformation;

import org.objectweb.asm.Type;

/**
 * Contextual information for transformations implemented by {@link SimpleClassProcessor},
 * {@link SimpleMethodProcessor} or {@link SimpleFieldProcessor}.
 */
public interface SimpleTransformationContext {
    /**
     * {@return the class being transformed}
     */
    Type type();

    /**
     * {@return true if the class does not exist and the given node is currently empty}
     *
     * <p>This is only relevant for processors targeting classes, since method and field processors
     * will never be invoked for classes that don't exist.
     */
    boolean empty();

    /**
     * {@return SHA-256 hash of the original untransformed class bytecode}
     */
    byte[] initialSha256();
}
