package io.izzel.arclight.common.mod.util;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecrell.terminalconsole.TerminalConsoleAppender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;

import java.util.List;
import java.util.concurrent.ExecutionException;

// see net.minecraftforge.server.console.TerminalHandler
public class ArclightTerminalReader {

    public static boolean handleCommands(DedicatedServer server) {
        var terminal = TerminalConsoleAppender.getTerminal();
        if (terminal == null)
            return false;

        var reader = LineReaderBuilder.builder()
            .appName("Arclight")
            .terminal(terminal)
            .completer(new ConsoleCommandCompleter(server))
            .build();
        reader.setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION);
        reader.unsetOpt(LineReader.Option.INSERT_TAB);

        TerminalConsoleAppender.setReader(reader);

        try {
            String line;
            while (!server.isStopped() && server.isRunning()) {
                try {
                    line = reader.readLine("> ");
                } catch (EndOfFileException ignored) {
                    // Continue reading after EOT
                    continue;
                }

                if (line == null)
                    break;

                line = line.trim();
                if (!line.isEmpty()) {
                    server.handleConsoleInput(line, server.createCommandSourceStack());
                }
            }
        } catch (UserInterruptException e) {
            // DONT USE CLOSE, YOU AREN'T GOING TO SHUT IT DOWN IN ANOTHER THREAD!
            server.halt(true);
        } finally {
            TerminalConsoleAppender.setReader(null);
        }
        return true;
    }

    public static Completer createCommandCompleter(DedicatedServer server) {
        return new ConsoleCommandCompleter(server);
    }

    static final class ConsoleCommandCompleter implements Completer {
        private static final Logger logger = LogManager.getLogger();
        private final DedicatedServer server;

        public ConsoleCommandCompleter(DedicatedServer server) {
            this.server = Preconditions.checkNotNull(server, "server");
        }

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            String buffer = line.line();
            final boolean prefix = !buffer.isEmpty() && buffer.charAt(0) == '/';
            if (!prefix) {
                buffer = '/' + buffer;
            }
            final String input = buffer;
            var craftServer = ((io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge) this.server).bridge$getServer();
            var asyncEvent = new com.destroystokyo.paper.event.server.AsyncTabCompleteEvent(craftServer.getConsoleSender(), input, true, null);
            // JLine owns this off-main call. Cancellation must not enter Brigadier.
            asyncEvent.callEvent();
            if (asyncEvent.isCancelled()) {
                return;
            }
            final java.util.concurrent.CompletableFuture<List<com.destroystokyo.paper.event.server.AsyncTabCompleteEvent.Completion>> prepared =
                new java.util.concurrent.CompletableFuture<>();
            final var provided = new java.util.ArrayList<>(asyncEvent.completions());
            ((io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge) this.server).bridge$queuedProcess(() -> {
                try {
                    if (asyncEvent.isHandled()) {
                        prepared.complete(provided);
                        return;
                    }
                    // Retain the native/mod dispatcher and its real future, but run
                    // permission checks and synchronous plugin providers on main.
                    StringReader command = new StringReader(input);
                    if (command.canRead() && command.peek() == '/') command.skip();
                    ParseResults<CommandSourceStack> parsed = this.server.getCommands().getDispatcher().parse(command, this.server.createCommandSourceStack());
                    this.server.getCommands().getDispatcher().getCompletionSuggestions(parsed).thenApply(suggestions -> {
                        var completions = new java.util.ArrayList<com.destroystokyo.paper.event.server.AsyncTabCompleteEvent.Completion>();
                        for (Suggestion suggestion : suggestions.getList()) {
                            var tooltip = suggestion.getTooltip() == null ? null : io.papermc.paper.adventure.PaperAdventure.asAdventure(
                                net.minecraft.network.chat.ComponentUtils.fromMessage(suggestion.getTooltip()));
                            completions.add(com.destroystokyo.paper.event.server.AsyncTabCompleteEvent.Completion.completion(suggestion.getText(), tooltip));
                        }
                        return completions;
                    }).whenComplete((completions, error) -> {
                        if (error != null) prepared.completeExceptionally(error);
                        else prepared.complete(completions);
                    });
                } catch (Throwable error) {
                    prepared.completeExceptionally(error);
                }
            });
            try {
                var completions = prepared.get(); // Wait only on JLine, never on main.
                var sync = new org.bukkit.craftbukkit.util.Waitable<List<String>>() {
                    @Override
                    protected List<String> evaluate() {
                        var labels = new java.util.ArrayList<String>();
                        for (var completion : completions) labels.add(completion.suggestion());
                        var event = new org.bukkit.event.server.TabCompleteEvent(craftServer.getConsoleSender(), input, labels);
                        return event.callEvent() ? new java.util.ArrayList<>(event.getCompletions()) : java.util.Collections.emptyList();
                    }
                };
                ((io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge) this.server).bridge$queuedProcess(sync);
                for (String completion : sync.get()) {
                    if (completion.isEmpty()) continue;
                    net.kyori.adventure.text.Component tooltip = null;
                    for (var original : completions) {
                        if (original.suggestion().equals(completion)) {
                            tooltip = original.tooltip();
                            break;
                        }
                    }
                    String value = prefix || completion.charAt(0) != '/' ? completion : completion.substring(1);
                    candidates.add(new Candidate(value, value, null,
                        io.papermc.paper.adventure.PaperAdventure.ANSI_SERIALIZER.serializeOr(tooltip, null), null, null, false));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                logger.error("Failed to tab complete", e);
            }
        }

    }
}
