/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.startup;

import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BaseMultiResolutionImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.i18n.FMLTranslations;
import net.neoforged.fml.loading.ImmediateWindowHandler;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

/**
 * Handles reporting of fatal errors for UI-based entrypoints (i.e. clients).
 */
public final class FatalErrorReporting {
    private FatalErrorReporting() {}

    private static Throwable unwrapException(Throwable t) {
        if (t == null) {
            return null;
        }
        var cause = t.getCause();
        if (cause == null) {
            return t; // Cannot unwrap without a cause.
        }

        if (t instanceof InvocationTargetException) {
            return cause;
        }
        return t;
    }

    public static void reportFatalError(Throwable t) {
        Path gameDir = null;
        if (t instanceof FatalStartupException fatalStartupException) {
            gameDir = fatalStartupException.getStartupArgs().gameDirectory();
        }
        reportFatalError(t, gameDir, null, null);
    }

    public static void reportFatalError(Throwable t, @Nullable Path gameDir, @Nullable Path logFile, @Nullable Path crashReport) {
        var issues = new ArrayList<ModLoadingIssue>();
        collectModLoadingIssues(t, issues);

        if (issues.isEmpty()) {
            t = unwrapException(t);

            if (t instanceof FatalStartupException e) {
                var errorText = new StringBuilder(e.getMessage());
                if (e.getCause() != null) {
                    errorText.append("\n\nTechnical Details:\n");
                    appendAbbreviatedExceptionChain(unwrapException(e.getCause()), errorText);
                }

                issues.add(ModLoadingIssue.error("fml.modloadingissue.technical_error", errorText).withCause(t));
            } else {
                var exceptionText = new StringBuilder();
                appendAbbreviatedExceptionChain(t, exceptionText);

                issues.add(ModLoadingIssue.error("fml.modloadingissue.technical_error", exceptionText).withCause(t));
            }
        }

        // This should exit after showing the error
        ImmediateWindowHandler.displayFatalErrorAndExit(
                issues,
                gameDir != null ? gameDir.resolve("mods") : null,
                logFile,
                crashReport);

        // When we get here, there was no immediate window provider loaded. We crashed before that got loaded.
        var errorReport = new StringBuilder();
        for (var issue : issues) {
            errorReport.append(FMLTranslations.translateIssue(issue));
        }
        reportFatalError(errorReport.toString());
    }

    private static void collectModLoadingIssues(Throwable t, List<ModLoadingIssue> issues) {
        if (t instanceof ModLoadingException e) {
            issues.addAll(e.getIssues());
        }

        for (var suppressed : t.getSuppressed()) {
            collectModLoadingIssues(suppressed, issues);
        }

        if (t.getCause() != null) {
            collectModLoadingIssues(t.getCause(), issues);
        }
    }

    private static void appendAbbreviatedExceptionChain(Throwable t, StringBuilder exceptionText) {
        boolean first = true;
        while (t != null) {
            if (first) {
                exceptionText.append("  ").append(t).append('\n');
                first = false;
            } else {
                exceptionText.append(" ↳").append(t).append('\n');
            }
            t = unwrapException(t.getCause());
        }
    }

    public static void reportFatalErrorOnConsole(Throwable t) {
        t = unwrapException(t);
        t.printStackTrace();
    }

    /**
     * Report a fatal startup error.
     * At this point, it doesn't matter if we double-classload from the system classloader, since we're about to exit.
     */
    public static void reportFatalError(String message) {
        System.setProperty("java.awt.headless", "false"); // Overriding what MC set
        if (!GraphicsEnvironment.isHeadless()) {
            showErrorUsingSwing(message);
        } else {
            // TinyFD refuses to let us use quotes
            message = message.replace('"', '`');
            message = message.replace('\'', '`');

            TinyFileDialogs.tinyfd_messageBox(
                    "NeoForge - Fatal Startup Error",
                    message,
                    "ok",
                    "error",
                    1);
        }
        System.exit(1);
    }

    private static void showErrorUsingSwing(String message) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        String html = "<html><body width='400'><strong>Fatal Startup Error</strong>"
                + "<p><pre>";
        html += escapeHtmlContent(message);

        var icon = new ImageIcon(createMultiResImage());
        JOptionPane.showMessageDialog(null, html, "Fatal Error", JOptionPane.ERROR_MESSAGE, icon);
    }

    private static BaseMultiResolutionImage createMultiResImage() {
        var images = new ArrayList<Image>();
        readImage("crash-32x32.png", images);
        readImage("crash-64x64.png", images);
        readImage("crash-128x128.png", images);
        return new BaseMultiResolutionImage(images.toArray(Image[]::new));
    }

    private static void readImage(String filename, List<Image> images) {
        try {
            var image = ImageIO.read(FatalErrorReporting.class.getResource(filename));
            images.add(image);
        } catch (IOException ignored) {}
    }

    /**
     * & &amp;
     * < &lt;
     * > &gt;
     * " &quot;
     * ' &#x27;
     */
    private static String escapeHtmlContent(String content) {
        return content
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
