package us.jcedeno.deltauhc.bukkit.stages.lobby.commands;

import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.ProxiedBy;
import cloud.commandframework.annotations.processing.CommandContainer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import us.jcedeno.deltauhc.bukkit.DeltaUHC;

@CommandContainer
@CommandDescription("lobby commands test")
public class LobbyCommands {
    public static MiniMessage mini = MiniMessage.miniMessage();

    public LobbyCommands(final AnnotationParser<CommandSender> parser) {
    }

    @CommandMethod("stuck")
    public void stuckCommand(final CommandSender sender) {
        if (DeltaUHC.gameConfig().getCurrentGameTime() > 20) {
            sender.sendMessage(mini.deserialize("<red>You can't use <white>stuck</white> after second 20!"));
            return;
        }
        if (sender instanceof Player p) {
            // TODO: Stop spam of this command, add unstuckall command
            p.sendMessage(mini.deserialize("<green>You've been unstuck!"));
            p.teleport(p.getLocation().getBlock().getRelative(BlockFace.UP).getLocation().add(0.5, 0.5, 0.5));
        }
    }

    @CommandMethod("lobby")
    public void containerCommand(final CommandSender sender) {
        if (sender instanceof Player p) {
            p.sendMessage(mini.deserialize("<rainbow>Game config: <yellow>" + p.getName() + "</yellow>!"));
            p.sendMessage(DeltaUHC.gameConfig().toString());
            return;
        }
        sender.sendMessage(mini.deserialize("<red>Console can't run this command."));
    }

    @ProxiedBy("settings-startPlayers")
    @CommandMethod("lobby settings startPlayers <number>")
    public void lobbySettings(final CommandSender sender, @Argument(value = "number") Integer number) {
        sender.sendMessage(mini
                .deserialize("<green>Changed amount of players required to start to <white>" + number + "</white>!"));

        DeltaUHC.gameConfig().setStartPlayers(number);
    }

    @ProxiedBy("settings-radius")
    @CommandMethod("lobby settings radius <number>")
    public void gameRadius(final CommandSender sender, @Argument(value = "number") Integer number) {
        sender.sendMessage(mini
                .deserialize("<green>Changed game radius to <white>" + number + "</white>!"));

        DeltaUHC.gameConfig().setRadius(number);
    }

    @ProxiedBy("settings-shrink-time")
    @CommandMethod("lobby settings shrink-time <seconds>")
    public void shrinkTime(final CommandSender sender, @Argument(value = "seconds") Integer seconds) {
        sender.sendMessage(mini
                .deserialize("<green>Changed game shrink time to <white>" + seconds + "</white>!"));

        DeltaUHC.gameConfig().setShrinkStartTime(seconds);
    }

    @CommandMethod("lobby start")
    @ProxiedBy("start")
    public void startCommand(final CommandSender sender) {
        if (!sender.hasPermission("uhc.start")) {
            sender.sendMessage(mini.deserialize("<red>You are not authorized to use this command."));
            return;
        }
        // Register the ingame task
        sender.sendMessage("Starting the game...");
        DeltaUHC.getGame().getInGameStage().registerTasks();

    }

}
