package us.jcedeno.deltauhc.bukkit.config.commands;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.stream.Stream;

import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.ProxiedBy;
import cloud.commandframework.annotations.processing.CommandContainer;
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import us.jcedeno.deltauhc.bukkit.DeltaUHC;
import us.jcedeno.deltauhc.bukkit.common.utils.StringUtils;
import us.jcedeno.deltauhc.bukkit.config.GameConfig;
import java.util.stream.Collectors;
import java.util.Comparator;

/**
 * Class that holds
 */
@CommandContainer
@CommandDescription("Command to check and modify the game settings.")
public class SettingsCommand {

    @Suggestions("stgs")
    public @NonNull List<String> getSettings(
            final @NonNull CommandContext<CommandSender> ctx,
            final @NonNull String input) {
        return Stream.of(GameConfig.class.getDeclaredFields()).map(f -> f.getName()).toList();
    }

    @CommandMethod("settings")
    @ProxiedBy("config")
    public void settings(CommandSender sender) {

        var config = DeltaUHC.gameConfig();
        var clazz = config.getClass();

        String settingsSerialized = Stream.of(clazz.getDeclaredFields())
                .filter(f -> !f.isAnnotationPresent(GameConfig.IgnoreSetting.class))
                .sorted(Comparator.comparing(Field::getName))
                .map(f -> {
                    var name = f.getName();
                    var type = f.getType();
                    var prefix = type == boolean.class || type == Boolean.class ? "is" : "get";
                    var methodName = prefix + name.substring(0, 1).toUpperCase() + name.substring(1);

                    try {
                        var method = clazz.getDeclaredMethod(methodName);
                        var getterResponse = method.invoke(config);

                        var displayName = separateCamelCase(name);
                        var actualDisplayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);

                        if ((name.toLowerCase().endsWith("time") || name.toLowerCase().endsWith("duration"))
                                && (type == int.class || type == Integer.class)) {
                            String formattedTime = StringUtils.formatTime((int) getterResponse);
                            return "<green>" + actualDisplayName + ": <white>" + formattedTime + "\n";
                        } else {
                            return "<green>" + actualDisplayName + ": <white>" + getterResponse + "\n";
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "<red>Error retrieving setting for " + name + "</red>\n";
                    }
                })
                .collect(Collectors.joining());

        sender.sendMessage(
                miniMessage().deserialize("\n<gold><bold>Current Game Config:</bold>" + "\n\n" + settingsSerialized));

    }

    @CommandMethod("secret-config")
    @CommandPermission("uhc.settings.secret")
    @ProxiedBy("config-secret")
    public void secretSettings(CommandSender sender) {

        var config = DeltaUHC.gameConfig();
        var clazz = config.getClass();

        String settingsSerialized = Stream.of(clazz.getDeclaredFields())
                .sorted(Comparator.comparing(Field::getName))
                .map(f -> {
                    var name = f.getName();
                    var type = f.getType();
                    var prefix = type == boolean.class || type == Boolean.class ? "is" : "get";
                    var methodName = prefix + name.substring(0, 1).toUpperCase() + name.substring(1);

                    try {
                        var method = clazz.getDeclaredMethod(methodName);
                        var getterResponse = method.invoke(config);

                        var displayName = separateCamelCase(name);
                        var actualDisplayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);

                        if ((name.toLowerCase().endsWith("time") || name.toLowerCase().endsWith("duration"))
                                && (type == int.class || type == Integer.class)) {
                            String formattedTime = StringUtils.formatTime((int) getterResponse);
                            return "<green>" + actualDisplayName + ": <white>" + formattedTime + "\n";
                        } else {
                            return "<green>" + actualDisplayName + ": <white>" + getterResponse + "\n";
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "<red>Error retrieving setting for " + name + "</red>\n";
                    }
                })
                .collect(Collectors.joining());

        sender.sendMessage(miniMessage()
                .deserialize("<gold><bold><rainbow>SECRET</rainbow> Settings:</bold>" + "\n\n" + settingsSerialized));

    }

    private String separateCamelCase(String s) {
        return s.replaceAll(String.format("%s|%s|%s",
                "(?<=[A-Z])(?=[A-Z][a-z])",
                "(?<=[^A-Z])(?=[A-Z])",
                "(?<=[A-Za-z])(?=[^A-Za-z])"), " ").replaceAll(" +", " ").trim();
    }

    @ProxiedBy("config")
    @CommandPermission("uhc.config.modify")
    @CommandMethod("settings <field> set <value>")
    public void changeSettings(CommandSender sender,
            @Argument(value = "field", suggestions = "stgs") String field,
            @Argument(value = "value") @Greedy String value) {

        try {
            Field f = GameConfig.class.getDeclaredField(field);

            Object parsedValue = parseValue(f.getType(), value);
            if (parsedValue != null) {
                
                var method = GameConfig.class.getDeclaredMethod(
                        "set" + f.getName().substring(0, 1).toUpperCase() + f.getName().substring(1), f.getType());

                method.invoke(DeltaUHC.gameConfig(), parsedValue);
                sender.sendMessage(miniMessage().deserialize("<green>Config updated successfully!</green>"));
            } else {
                sender.sendMessage(
                        miniMessage().deserialize("<red>Invalid value type for the specified field.</red>"));
            }

        } catch (NoSuchFieldException e) {
            sender.sendMessage(miniMessage().deserialize("<red>The specified field does not exist.</red>"));
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            sender.sendMessage(miniMessage().deserialize("<red>Access to the specified field is denied.</red>"));
            e.printStackTrace();
        } catch (Exception e) {
            sender.sendMessage(miniMessage().deserialize("<red>Something went wrong updating the config.</red>"));
            e.printStackTrace();
        }
    }

    private Object parseValue(Class<?> type, String value) {
        try {
            if (type == int.class || type == Integer.class) {
                return Integer.parseInt(value);
            } else if (type == boolean.class || type == Boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (type == double.class || type == Double.class) {
                return Double.parseDouble(value);
            } else if (type == float.class || type == Float.class) {
                return Float.parseFloat(value);
            } else if (type == long.class || type == Long.class) {
                return Long.parseLong(value);
            } else if (type == String.class) {
                return value;
            } else {
                // Add more type parsing as needed
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public SettingsCommand(final AnnotationParser<CommandSender> annotationParser) {
    }
}
