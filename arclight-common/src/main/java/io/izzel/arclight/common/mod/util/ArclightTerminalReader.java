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
            server.close();
        } finally {
            TerminalConsoleAppender.setReader(null);
        }
        return true;
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
            boolean prefix;
            if (buffer.isEmpty() || buffer.charAt(0) != '/') {
                buffer = '/' + buffer;
                prefix = false;
            } else {
                prefix = true;
            }

            final String input = buffer;
            //See NetHandlerPlayServer#processTabComplete
            StringReader stringReader = new StringReader(input);
            if (stringReader.canRead() && stringReader.peek() == '/')
                stringReader.skip();

            try {
                ParseResults<CommandSourceStack> results = this.server.getCommands().getDispatcher().parse(stringReader, this.server.createCommandSourceStack());
                Suggestions tabComplete = this.server.getCommands().getDispatcher().getCompletionSuggestions(results).get();
                for (Suggestion suggestion : tabComplete.getList()) {
                    String completion = suggestion.getText();
                    if (!completion.isEmpty()) {
                        boolean hasPrefix = prefix || completion.charAt(0) != '/';
                        Candidate candidate = new Candidate(hasPrefix ? completion : completion.substring(1));
                        candidates.add(candidate);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                logger.error("Failed to tab complete", e);
            }
        }

    }
}
