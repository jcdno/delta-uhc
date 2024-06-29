package us.jcedeno.deltauhc.bukkit.stages.lobby.commands;

import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import cloud.commandframework.annotations.AnnotationParser;
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
            p.sendMessage(mini.deserialize("<green>You've been unstuck!"));
            p.teleport(p.getLocation().getBlock().getRelative(BlockFace.UP).getLocation().add(0.5, 0.5, 0.5));
        }
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
        DeltaUHC.getGame().getStartingStage().registerTasks();

    }

}
