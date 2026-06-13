/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import org.jetbrains.annotations.Nullable;

/**
 * A config spec is responsible for interpreting (loading, correcting) raw {@link CommentedConfig}s from NightConfig.
 *
 * <p>NeoForge provides {@code ModConfigSpec} for the most common cases.
 *
 * <h3>Thread-safety</h3>
 * <p>The {@link Config} objects themselves are thread-safe, but mod config code should not be assumed to be thread-safe in general.
 * FML will guard event dispatches behind a lock when necessary.
 * A spec can safely mutate {@code Config} objects, but should let FML fire events and saving the file by calling {@link ILoadedConfig#save()}.
 */
public interface IConfigSpec {
    /**
     * Returns {@code true} if this spec is empty.
     */
    boolean isEmpty();

    /**
     * Validate this specification in the context of the given {@code config}.
     *
     * @param config the configuration this spec is used by
     */
    void validateSpec(ModConfig config);

    /**
     * Checks that a config is correct.
     * If this function returns {@code false},
     * a backup is made (except for initial creation) then the config is fed through {@link #correct}.
     */
    boolean isCorrect(UnmodifiableCommentedConfig config);

    /**
     * Corrects a config. Only called if {@link #isCorrect} returned {@code false}.
     *
     * <p>This can be used to fix broken entries, add back missing entries or comments, etc...
     * The returned config will be saved to disk.
     *
     * <p>This function is also used to construct the default instance of a config, by passing in an empty config.
     *
     * <p>The config should not be loaded into the spec yet. A call to {@link #acceptConfig} will be made for that.
     *
     * <p>The config should not be saved yet. FML will take care of that after this method.
     */
    void correct(CommentedConfig config);

    /**
     * Updates the spec's data to a config.
     * This is called on loading and on reloading.
     * The config is guaranteed to be valid according to {@link #isCorrect}.
     */
    void acceptConfig(@Nullable ILoadedConfig config);

    sealed interface ILoadedConfig permits LoadedConfig {
        /**
         * Accesses the current config.
         *
         * <p>The config locks internally, and is therefore safe to
         * read/write across multiple threads without additional synchronization.
         */
        CommentedConfig config();

        /**
         * Saves the current value of the {@link #config} and dispatches a config reloading event.
         */
        void save();
    }
}
