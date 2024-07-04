package us.jcedeno.deltauhc.bukkit.stages.ingame.tasks;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static us.jcedeno.deltauhc.bukkit.common.utils.StringUtils.formatTime;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import us.jcedeno.deltauhc.bukkit.DeltaUHC;
import us.jcedeno.deltauhc.bukkit.config.GameConfig;
import us.jcedeno.deltauhc.bukkit.locations.Locations;
import us.jcedeno.deltauhc.bukkit.stages.ingame.InGameStage;
import us.jcedeno.deltauhc.bukkit.team.models.Team;

public class GameLoop implements Runnable {
    private final GameConfig cfg;
    private final InGameStage stage;
    private final MiniMessage mini = miniMessage();
    private boolean isBorderShrinking;

    public GameLoop(final GameConfig cfg, final InGameStage stage) {
        this.cfg = cfg;
        this.stage = stage;
        this.isBorderShrinking = false;
    }

    @Override
    public void run() {
        var time = cfg.increaseGameTime();

        if (time == cfg.getShrinkStartTime()) {
            // Start border shrink in all worlds except the lobby
            isBorderShrinking = true;
            Bukkit.getScheduler().runTask(DeltaUHC.getGame(), () -> {
                double finalSize = cfg.getRadiusFinalSize() * 2;
                long duration = cfg.getShrinkDuration();
                Bukkit.getWorlds().stream()
                        .filter(world -> !world.getName().equals("lobby"))
                        .forEach(world -> world.getWorldBorder().setSize(finalSize, duration));
            });
            Bukkit.broadcast(
                    mini.deserialize("<yellow>[!!] The world border will begin to shrink and it will take <white>"
                            + formatTime(cfg.getShrinkDuration())
                            + "</white> to complete shrinking down to a radius of <white>"
                            + cfg.getRadiusFinalSize() + "</white> blocks!</yellow>"));
            Bukkit.getOnlinePlayers().forEach(p -> p.playNote(p.getLocation(), Instrument.CHIME, Note.flat(1, Tone.A)));

            // Schedule task to run after shrink duration to handle post-shrink actions
            Bukkit.getScheduler().runTaskLater(DeltaUHC.getGame(), () -> {
                cfg.setNether(false);
                cfg.setEnd(false);
                isBorderShrinking = false;

                World overworld = Locations.getGameWorld();
                double radiusFinalSize = cfg.getRadiusFinalSize();
                Map<String, List<Player>> playersByTeam = DeltaUHC.getGame().getTeamManager().getPlayersByTeam();
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.getWorld().getEnvironment() == World.Environment.NETHER
                                || p.getWorld().getEnvironment() == World.Environment.THE_END)
                        .collect(Collectors.groupingBy(DeltaUHC.getGame().getTeamManager()::teamByPlayer,
                                Collectors.toList()))
                        .forEach((team, players) -> {
                            Location teleportLocation = findSafeLocationForTeam(overworld, radiusFinalSize, team,
                                    playersByTeam);
                            players.forEach(p -> {
                                p.teleport(teleportLocation);
                                p.sendMessage(mini.deserialize(
                                        "<yellow>You have been teleported to the overworld because the border has shrunk.</yellow>"));
                            });
                        });
            }, cfg.getShrinkDuration() * 20); // Convert seconds to ticks
        }

        if (time == cfg.getHealTime()) {
            DeltaUHC.runSync(() -> {
                Bukkit.getOnlinePlayers().forEach(p -> {
                    p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                    p.setFoodLevel(20);
                });
                Bukkit.broadcast(mini.deserialize("<#89ff9f>[+] Final heal"));
            });
        }

        if (time == cfg.getPvpTime()) {
            cfg.setPvp(true);
            Bukkit.broadcast(mini.deserialize("<#DC14A0>[⚔] PVP has been enabled."));
        }

        int timeUntilHeal = getTimeUntilEvent(time, cfg.getHealTime());
        int timeUntilPvP = getTimeUntilEvent(time, cfg.getPvpTime());
        int timeUntilShrink = getTimeUntilEvent(time, cfg.getShrinkStartTime());

        int timeUntilNextEvent = Math.min(Math.min(timeUntilHeal, timeUntilPvP), timeUntilShrink);

        String nextEventName = null;
        if (timeUntilNextEvent != Integer.MAX_VALUE) {
            if (timeUntilNextEvent == timeUntilHeal) {
                nextEventName = "Final Heal";
            } else if (timeUntilNextEvent == timeUntilPvP) {
                nextEventName = "PvP";
            } else if (timeUntilNextEvent == timeUntilShrink) {
                nextEventName = "Border Shrink";
            }
        }

        final Component nextEvent = nextEventName == null ? null
                : mini.deserialize(nextEventName + ": <green>" + formatTime(timeUntilNextEvent));

        // Check if the player is close to the border
        double borderDistanceWarning = cfg.getBorderDistanceWarning();

        Bukkit.getOnlinePlayers().forEach(p -> {
            if (isBorderShrinking) {
                WorldBorder border = p.getWorld().getWorldBorder();
                double borderSize = border.getSize() / 2;
                double distanceToBorder = Math.min(
                        Math.min(Math.abs(p.getLocation().getX() - border.getCenter().getX()),
                                borderSize - Math.abs(p.getLocation().getX() - border.getCenter().getX())),
                        Math.min(Math.abs(p.getLocation().getZ() - border.getCenter().getZ()),
                                borderSize - Math.abs(p.getLocation().getZ() - border.getCenter().getZ())));

                if (distanceToBorder <= borderDistanceWarning) {
                    // Make the message Warning: 1.0 from border
                    var msg = mini
                            .deserialize("<red>Warning: <white>" + String.format("%.2f", distanceToBorder)
                                    + "m</white> from border!</red>");
                    if (nextEvent != null) {
                        msg.append(mini.deserialize("<gray> - </gray>").append(nextEvent));
                    }
                    p.sendActionBar(msg);
                }
            } else {
                p.sendActionBar(nextEvent);
            }

            DeltaUHC.getGame().getBoard(p).updateLines(stage.getProcessedLines());
        });
    }

    private int getTimeUntilEvent(int currentTime, int eventTime) {
        return eventTime > currentTime ? eventTime - currentTime : Integer.MAX_VALUE;
    }

    private Location findSafeLocationForTeam(World overworld, double radiusFinalSize, Team team,
            Map<String, List<Player>> playersByTeam) {
        if (team == null) {
            return getRandomSafeLocation(overworld, radiusFinalSize);
        }

        List<Player> teamMembersInOverworld = playersByTeam.getOrDefault(team, List.of()).stream()
                .filter(p -> p.getWorld().getEnvironment() == World.Environment.NORMAL)
                .collect(Collectors.toList());

        if (!teamMembersInOverworld.isEmpty()) {
            return teamMembersInOverworld.get(0).getLocation();
        } else {
            return getRandomSafeLocation(overworld, radiusFinalSize);
        }
    }

    private Location getRandomSafeLocation(World overworld, double radiusFinalSize) {
        double x = -radiusFinalSize + Math.random() * (radiusFinalSize * 2);
        double z = -radiusFinalSize + Math.random() * (radiusFinalSize * 2);
        double y = overworld.getHighestBlockYAt((int) x, (int) z);
        return new Location(overworld, x, y, z);
    }
}
