package us.jcedeno.deltauhc.bukkit.stages.lobby.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.processing.CommandContainer;
import net.kyori.adventure.text.minimessage.MiniMessage;

@CommandContainer
@CommandDescription("lobby commands test")
public class LobbyCommands {
    public static MiniMessage mini = MiniMessage.miniMessage();

    public LobbyCommands(final AnnotationParser<CommandSender> parser) {
    }

    @CommandMethod("lobby")
    public void containerCommand(final CommandSender sender) {
        if (sender instanceof Player p) {
            p.sendMessage(mini.deserialize("<rainbow>Lobby command ran <yellow>" + p.getName() + "</yellow>!"));
            return;
        }
        sender.sendMessage(mini.deserialize("<red>Console can't run this command."));
    }

}
