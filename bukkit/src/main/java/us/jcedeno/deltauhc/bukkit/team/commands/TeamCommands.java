package us.jcedeno.deltauhc.bukkit.team.commands;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.ProxiedBy;
import cloud.commandframework.annotations.processing.CommandContainer;
import cloud.commandframework.annotations.specifier.Greedy;
import lombok.extern.log4j.Log4j2;
import us.jcedeno.deltauhc.bukkit.DeltaUHC;
import us.jcedeno.deltauhc.bukkit.team.TeamManager;

/**
 * TODO:
 * - Add team size validation, teams cannot have more members than config.teamSize
 * - Make all accept and reject commands clickable.
 * - Add /tc to toggle between chat channels, if TC enabled, then all messages are on team chat by default, unless prefixed by '!'.
 */
@CommandContainer
@CommandDescription("Team commands")
@Log4j2
public class TeamCommands {
    public TeamManager teamManager = DeltaUHC.getGame().getTeamManager();

    private static final String DEFAULT_TEAM_NAME = "DefaultTeamName";

    @CommandMethod("teams")
    public void teams(final @NonNull CommandSender sender) {
        // If no teams, send a message saying that there are no teams
        if (teamManager.teams().isEmpty()) {
            sender.sendMessage(miniMessage().deserialize("<red>There are no teams."));
            return;
        }
        sender.sendMessage(miniMessage().deserialize("<green>Teams:"));
        // Send a list of all the teams
        teamManager.teams().forEach(team -> {
            sender.sendMessage(miniMessage().deserialize(String.format("<white>%s</white>", team.getTeamName())));
        });
    }

    @CommandMethod("team create [teamName]")
    @ProxiedBy("tcreate")
    @CommandDescription("If the  sender doesn't have a team, it create it for them")
    public void createTeam(final @NonNull Player player,
            @Argument(value = "teamName", defaultValue = DEFAULT_TEAM_NAME) @Greedy String teamName) {
        if (!DeltaUHC.gameConfig().isTeamManagement()) {
            player.sendMessage(miniMessage().deserialize(String.format("<red>Team management is disabled!")));
            return;
        }
        // If team name is actually default team name, then generate a random team name
        if (teamName.equals(DEFAULT_TEAM_NAME)) {
            teamName = "Team " + UUID.randomUUID().toString().split("-")[0];
            log.info("Team name was default, generating random team name: {}", teamName);
        }
        if (teamManager.hasTeam(player.getUniqueId())) {
            player.sendMessage(miniMessage().deserialize(String.format("<red>You already have a team.")));
            return;
        }
        // We need to check if the player already, is they do, then we exit early.
        player.sendMessage(miniMessage()
                .deserialize(String.format("<green>Team <white><bold>%s</bold></white> has been created.", teamName)));

        var newTeam = teamManager.createTeam(player.getUniqueId());

        newTeam.setTeamName(teamName);
        newTeam.setDisplayName(teamName);
    }

    /**
     * Disbands the team of the player.
     * 
     */
    @CommandMethod("team disband")
    @ProxiedBy("disband")
    @CommandDescription("Disbands the team of the player.")
    public void disbandTeam(final @NonNull Player player) {
        if (!DeltaUHC.gameConfig().isTeamManagement()) {
            player.sendMessage(miniMessage().deserialize(String.format("<red>Team management is disabled!")));
            return;
        }
        if (!teamManager.hasTeam(player.getUniqueId())) {
            player.sendMessage(miniMessage().deserialize("<red>You don't have a team."));
            return;
        }
        var team = teamManager.teamByPlayer(player.getUniqueId());
        teamManager.disbandTeam(team, player);
        player.sendMessage(miniMessage().deserialize("<green>Your team has been disbanded."));
    }

    @CommandMethod("team invite <target>")
    @ProxiedBy("invite")
    @CommandDescription("Invites a player to your team.")
    public void playerInviteCommand(final Player sender, final @Argument("target") OfflinePlayer target) {
        if (!DeltaUHC.gameConfig().isTeamManagement()) {
            sender.sendMessage(miniMessage().deserialize(String.format("<red>Team management is disabled!")));
            return;
        }
        if (!teamManager.hasTeam(sender.getUniqueId())) {
            sender.sendMessage(miniMessage().deserialize("<red>You don't have a team."));
            return;
        }
        if (teamManager.hasTeam(target.getUniqueId())) {
            sender.sendMessage(
                    miniMessage().deserialize(String.format("<red>%s already has a team.", target.getName())));
            return;
        }

        teamManager.sendTeamInvite(sender, target.getPlayer(), teamManager.teamByPlayer(sender.getUniqueId()));
    }

    @CommandMethod("team accept <inviter>")
    @ProxiedBy("accept")
    @CommandDescription("Accepts a team invite.")
    public void teamAcceptCommand(final Player sender, final @Argument("inviter") Player inviter) {

        if (!DeltaUHC.gameConfig().isTeamManagement()) {
            sender.sendMessage(miniMessage().deserialize(String.format("<red>Team management is disabled!")));
            return;
        }
        if (teamManager.hasTeam(sender.getUniqueId())) {
            sender.sendMessage(miniMessage().deserialize("<red>You already have a team."));
            return;
        }
        if (!teamManager.hasTeam(inviter.getUniqueId())) {
            sender.sendMessage(
                    miniMessage().deserialize(String.format("<red>%s doesn't have a team.", inviter.getName())));
            return;
        }
        teamManager.acceptTeamInvite(sender, inviter);
    }

    @CommandMethod("team deny <inviter>")
    @ProxiedBy("reject")
    @CommandDescription("Denies a team invite.")
    public void teamDenyCommand(final Player sender, final @Argument("inviter") Player inviter) {
        if (!DeltaUHC.gameConfig().isTeamManagement()) {
            sender.sendMessage(miniMessage().deserialize(String.format("<red>Team management is disabled!")));
            return;
        }
        if (teamManager.hasTeam(sender.getUniqueId())) {
            sender.sendMessage(miniMessage().deserialize("<red>You already have a team."));
            return;
        }
        if (!teamManager.hasTeam(inviter.getUniqueId())) {
            sender.sendMessage(
                    miniMessage().deserialize(String.format("<red>%s doesn't have a team.", inviter.getName())));
            return;
        }
        teamManager.rejectTeamInvite(sender, inviter);
    }

    @CommandMethod("team kick <target>")
    @CommandDescription("Kicks a player from your team.")
    public void teamKickCommand(final Player sender, final @Argument("target") OfflinePlayer target) {
        if (!DeltaUHC.gameConfig().isTeamManagement()) {
            sender.sendMessage(miniMessage().deserialize(String.format("<red>Team management is disabled!")));
            return;
        }
        if (!teamManager.hasTeam(sender.getUniqueId())) {
            sender.sendMessage(miniMessage().deserialize("<red>You don't have a team."));
            return;
        }
        if (!teamManager.hasTeam(target.getUniqueId())) {
            sender.sendMessage(
                    miniMessage().deserialize(String.format("<red>%s doesn't have a team.", target.getName())));
            return;
        }
        teamManager.kickPlayerFromTeam(sender, target.getPlayer());
    }

    @CommandMethod("team chat <msg>")
    @ProxiedBy("tc")
    @CommandDescription("Sends a message to your team.")
    public void teamChatCommand(final Player sender, final @Argument("msg") @Greedy String msg) {
        if (!teamManager.hasTeam(sender.getUniqueId())) {
            sender.sendMessage(miniMessage().deserialize("<red>You don't have a team."));
            return;
        }
        teamManager.sendTeamMessage(sender, msg);
    }

    // Boiler plate code for the command framework.
    public TeamCommands(final AnnotationParser<CommandSender> parser) {
    }

}
