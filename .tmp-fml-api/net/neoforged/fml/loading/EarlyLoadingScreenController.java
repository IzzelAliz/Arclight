/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading;

import org.jetbrains.annotations.Nullable;

/**
 * Interface for use by NeoForge to control the early loading screen.
 */
public interface EarlyLoadingScreenController {
    /**
     * Gets the current loading screen controller.
     */
    @Nullable
    static EarlyLoadingScreenController current() {
        return ImmediateWindowHandler.provider;
    }

    /**
     * Takes over ownership of the GLFW window created by the early loading screen.
     * <p>
     * This method can only be called once and once this method is called, any off-thread
     * interaction with the window seizes.
     *
     * @return The GLFW window handle for the window in a state that can be used by the game.
     */
    long takeOverGlfwWindow();

    /**
     * After calling {@linkplain #takeOverGlfwWindow() taking over} the main window, the game may still want to
     * periodically ask the loading screen to update itself independently. It will call this method to do so.
     */
    void periodicTick();

    /**
     * Sets a label for the main progress bar on the early loading screen.
     */
    void updateProgress(String label);

    /**
     * Clears all current progress in preparation for drawing a Minecraft overlay on top of the loading
     * screen.
     */
    void completeProgress();
}
