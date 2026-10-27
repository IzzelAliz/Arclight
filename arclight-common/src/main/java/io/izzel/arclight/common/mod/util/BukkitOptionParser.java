package io.izzel.arclight.common.mod.util;

import joptsimple.OptionDescriptor;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;

public class BukkitOptionParser extends OptionParser {

    public BukkitOptionParser() {
        acceptsAll(asList("c", "config"), "Properties file to use")
            .withRequiredArg()
            .ofType(File.class)
            .defaultsTo(new File("server.properties"))
            .describedAs("Properties file");

        acceptsAll(asList("P", "plugins"), "Plugin directory to use")
            .withRequiredArg()
            .ofType(File.class)
            .defaultsTo(new File("plugins"))
            .describedAs("Plugin directory");

        acceptsAll(asList("h", "host", "server-ip"), "Host to listen on")
            .withRequiredArg()
            .ofType(String.class)
            .describedAs("Hostname or IP");

        acceptsAll(asList("W", "world-dir", "universe", "world-container"), "World container")
            .withRequiredArg()
            .ofType(File.class)
            .describedAs("Directory containing worlds");

        acceptsAll(asList("w", "world", "level-name"), "World name")
            .withRequiredArg()
            .ofType(String.class)
            .describedAs("World name");

        acceptsAll(asList("p", "port", "server-port"), "Port to listen on")
            .withRequiredArg()
            .ofType(Integer.class)
            .describedAs("Port");

        acceptsAll(asList("o", "online-mode"), "Whether to use online authentication")
            .withRequiredArg()
            .ofType(Boolean.class)
            .describedAs("Authentication");

        acceptsAll(asList("s", "size", "max-players"), "Maximum amount of players")
            .withRequiredArg()
            .ofType(Integer.class)
            .describedAs("Server size");

        acceptsAll(asList("d", "date-format"), "Format of the date to display in the console (for log entries)")
            .withRequiredArg()
            .ofType(SimpleDateFormat.class)
            .describedAs("Log date format");

        acceptsAll(asList("log-pattern"), "Specfies the log filename pattern")
            .withRequiredArg()
            .ofType(String.class)
            .defaultsTo("server.log")
            .describedAs("Log filename");

        acceptsAll(asList("log-limit"), "Limits the maximum size of the log file (0 = unlimited)")
            .withRequiredArg()
            .ofType(Integer.class)
            .defaultsTo(0)
            .describedAs("Max log size");

        acceptsAll(asList("log-count"), "Specified how many log files to cycle through")
            .withRequiredArg()
            .ofType(Integer.class)
            .defaultsTo(1)
            .describedAs("Log count");

        acceptsAll(asList("log-append"), "Whether to append to the log file")
            .withRequiredArg()
            .ofType(Boolean.class)
            .defaultsTo(true)
            .describedAs("Log append");

        acceptsAll(asList("log-strip-color"), "Strips color codes from log file");

        acceptsAll(asList("b", "bukkit-settings"), "File for bukkit settings")
            .withRequiredArg()
            .ofType(File.class)
            .defaultsTo(new File("bukkit.yml"))
            .describedAs("Yml file");

        acceptsAll(asList("C", "commands-settings"), "File for command settings")
            .withRequiredArg()
            .ofType(File.class)
            .defaultsTo(new File("commands.yml"))
            .describedAs("Yml file");

        acceptsAll(asList("forceUpgrade"), "Whether to force a world upgrade");
        acceptsAll(asList("eraseCache"), "Whether to force cache erase during world upgrade");
        acceptsAll(asList("recreateRegionFiles"), "Whether to recreate region files during world upgrade");

        acceptsAll(asList("nojline"), "Disables jline and emulates the vanilla console");

        acceptsAll(asList("noconsole"), "Disables the console");

        acceptsAll(asList("demo"), "Demo mode");

        // Spigot Start
        acceptsAll(asList("S", "spigot-settings"), "File for spigot settings")
            .withRequiredArg()
            .ofType(File.class)
            .defaultsTo(new File("spigot.yml"))
            .describedAs("Yml file");

        accepts("add-plugin", "Additional plugin jar to load")
            .withRequiredArg()
            .ofType(File.class)
            .describedAs("Jar file");

        acceptsAll(asList("add-plugin-dir", "add-extra-plugin-dir"), "Additional plugin directories to load")
            .withRequiredArg()
            .ofType(File.class)
            .defaultsTo(new File[] {})
            .describedAs("Plugin directory");

        allowsUnrecognizedOptions();
    }

    public static String[] filterNativeArguments(OptionParser nativeParser, OptionSet bukkitOptions, String[] args) {
        Map<String, OptionSpec<?>> nativeSpecs = nativeParser.recognizedOptions();
        Map<String, OptionSpec<?>> bukkitSpecs = new BukkitOptionParser().recognizedOptions();
        Set<OptionSpec<?>> parsedSpecs = new HashSet<>(bukkitOptions.specs());
        ParsedOption[] bukkitArguments = new ParsedOption[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--")) {
                break;
            }
            ParsedOption option = parseOption(bukkitSpecs, args[i]);
            if (option != null && parsedSpecs.contains(option.spec())) {
                // Track occurrences, not just names: another occurrence can be an option's value.
                bukkitArguments[i] = option;
                if (option.consumesNextArgument()) {
                    i++;
                }
            }
        }

        var filtered = new ArrayList<String>(args.length);
        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            if (argument.equals("--")) {
                // A terminator consumed as a required value is handled below, not here.
                for (; i < args.length; i++) {
                    filtered.add(args[i]);
                }
                break;
            }

            ParsedOption nativeOption = parseOption(nativeSpecs, argument);
            if (nativeOption != null) {
                // Native-only options may take a Bukkit-looking token as their value.
                filtered.add(argument);
                if (nativeOption.consumesNextArgument() && i + 1 < args.length) {
                    filtered.add(args[++i]);
                }
                continue;
            }

            ParsedOption bukkitOption = bukkitArguments[i];
            if (bukkitOption != null) {
                if (bukkitOption.consumesNextArgument() && i + 1 < args.length) {
                    i++;
                }
            } else {
                // Keep unknown tokens so the original strict parser still rejects them.
                filtered.add(argument);
            }
        }
        return filtered.toArray(String[]::new);
    }

    private static ParsedOption parseOption(Map<String, OptionSpec<?>> specs, String argument) {
        if (!argument.startsWith("-") || argument.equals("-") || argument.equals("--")) {
            return null;
        }
        int prefixLength = argument.startsWith("--") ? 2 : 1;
        int equals = argument.indexOf('=', prefixLength);
        String name = argument.substring(prefixLength, equals < 0 ? argument.length() : equals);
        OptionSpec<?> spec = resolveOption(specs, name);
        if (spec != null) {
            return new ParsedOption(spec, equals < 0 && ((OptionDescriptor) spec).requiresArgument());
        }
        if (prefixLength == 2) {
            return null;
        }
        // jopt-simple tries a whole short name before considering a short-option cluster.
        for (int i = 1; i < argument.length(); i++) {
            spec = resolveOption(specs, argument.substring(i, i + 1));
            if (spec == null) {
                return null;
            }
            OptionDescriptor descriptor = (OptionDescriptor) spec;
            if (descriptor.acceptsArguments()) {
                return new ParsedOption(spec, i == argument.length() - 1 && descriptor.requiresArgument());
            }
        }
        return new ParsedOption(spec, false);
    }

    private static OptionSpec<?> resolveOption(Map<String, OptionSpec<?>> specs, String name) {
        OptionSpec<?> exact = specs.get(name);
        if (exact != null) {
            return exact;
        }
        OptionSpec<?> abbreviated = null;
        for (var entry : specs.entrySet()) {
            if (entry.getKey().startsWith(name)) {
                if (abbreviated != null) {
                    return null;
                }
                abbreviated = entry.getValue();
            }
        }
        return abbreviated;
    }

    private record ParsedOption(OptionSpec<?> spec, boolean consumesNextArgument) {
    }
}
