/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml;

import java.util.function.Supplier;

/**
 * An extension point for a mod container.
 *
 * <p>An extension point can be registered for a mod container using {@link ModContainer#registerExtensionPoint(Class, Supplier)}
 * and retrieved (if present) using {@link ModContainer#getCustomExtension(Class)}. An extension point allows a mod to
 * supply an arbitrary value to another mod or framework through their mod container class, avoiding
 * the use of {@link InterModComms} or other external frameworks to pass around these values.</p>
 */
public interface IExtensionPoint {}
