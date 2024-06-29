package us.jcedeno.deltauhc.bukkit.stages.ingame;

import static us.jcedeno.deltauhc.bukkit.common.utils.StringUtils.formatTime;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import us.jcedeno.deltauhc.bukkit.DeltaUHC;
import us.jcedeno.deltauhc.bukkit.locations.Locations;
import us.jcedeno.deltauhc.bukkit.stages.global.AbstractStage;
import us.jcedeno.deltauhc.bukkit.stages.ingame.listeners.ScatterListeners;
import us.jcedeno.deltauhc.bukkit.team.models.Team;

public class InGameStage extends AbstractStage implements Listener {
    public static MiniMessage mini = MiniMessage.miniMessage();
    public ScatterListeners scatterListeners = new ScatterListeners();
    private Integer taskId;

    /**
     * The template for the board that will be rendered in game.
     */
    private static final String GAME_BOARD = """
            <white>Time: <gold>%formattedGameTime%</gold>

            Alive: <yellow>%playersAlive%</yellow><gray>/</gray><gold>%initialPlayers%</gold>
            Border: <gold>%currentBorder%</gold>

            <rainbow>@thejcedeno</rainbow>
            """;

    /**
     * 
     * @return Returns the stage's board to be rendered
     */
    private static List<Component> getProcessedLines() {
        var config = DeltaUHC.gameConfig();

        String processedBoard = GAME_BOARD
                .replace("%formattedGameTime%", formatTime(config.getCurrentGameTime()))
                .replace("%playersAlive%", "" + config.getPlayersAlive().size())
                .replace("%initialPlayers%", "" + config.getInitialPlayers())
                .replace("%currentBorder%",
                        "" + String.format("%.2f", Locations.getGameWorld().getWorldBorder().getSize() / 2));

        return Arrays.stream(processedBoard.split("\n")).map(m -> mini.deserialize(m)).toList();
    }

    @Override
    public void registerTasks() {
        // Unregister lobby
        DeltaUHC.getGame().getLobbyStage().unregisterTasks();
        // Set world border
        var border = Locations.getGameWorld().getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(DeltaUHC.gameConfig().getRadius() * 2);
        // Adjust time, difficulty, health regen
        Locations.getGameWorld().setTime(0);
        Locations.getGameWorld().setDifficulty(Difficulty.PEACEFUL);
        Locations.getGameWorld().setGameRule(GameRule.NATURAL_REGENERATION, false);
        // Register tp protections
        Bukkit.getPluginManager().registerEvents(scatterListeners, DeltaUHC.getGame());
        // Scatter players
        startTeleport();

        Bukkit.getScheduler().runTaskLater(DeltaUHC.getGame(), () -> {
            HandlerList.unregisterAll(scatterListeners);
            Locations.getGameWorld().setDifficulty(Difficulty.HARD);
        }, 20 * 3);
        // Register this class as a listener
        Bukkit.getPluginManager().registerEvents(this, DeltaUHC.getGame());
        // TODO: Register a task that handles the whole start sequence and waiting for
        // start stage

        final AtomicBoolean firstTime = new AtomicBoolean(true);
        // wait for everyone to start
        final var cfg = DeltaUHC.gameConfig();

        // Start the game timer
        BukkitTask runTaskTimer = Bukkit.getScheduler().runTaskTimerAsynchronously(DeltaUHC.getGame(), () -> {
            var time = cfg.increaseGameTime();
            // Do second driven game loop here?

            if (time == cfg.getShrinkStartTime()) {
                // Start border shrink
                Bukkit.getScheduler().runTask(DeltaUHC.getGame(),
                        () -> Locations.getGameWorld().getWorldBorder().setSize(
                                cfg.getRadiusFinalSize() * 2,
                                cfg.getShrinkDuration()));
                Bukkit.broadcast(
                        mini.deserialize("<yellow>[!!] The world border will begin to shrink and it will take <white>"
                                + formatTime(cfg.getShrinkDuration())
                                + "</white> to complete shrinking down to a radius of <white>"
                                + cfg.getRadiusFinalSize() + "</white> blocks!</yellow>"));
                Bukkit.getOnlinePlayers()
                        .forEach(p -> p.playNote(p.getLocation(), Instrument.CHIME, Note.flat(1, Tone.A)));
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
            if (timeUntilNextEvent == timeUntilHeal) {
                nextEventName = "Final Heal";
            } else if (timeUntilNextEvent == timeUntilPvP) {
                nextEventName = "PvP";
            } else if (timeUntilNextEvent == timeUntilShrink) {
                nextEventName = "Border Shrink";
            }
            final String actualEvent = nextEventName;

            Bukkit.getOnlinePlayers().stream().forEach((p) -> {
                if (firstTime.get()) {
                    firstTime.set(false);
                    p.sendMessage(mini.deserialize("<green>The game has begun!"));
                    Bukkit.getScheduler().runTask(DeltaUHC.getGame(),
                            () -> {
                                p.clearActivePotionEffects();
                                p.getInventory().clear();
                                p.getInventory().addItem(ItemStack.of(Material.COOKED_BEEF, 8));
                                p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                                p.setGameMode(GameMode.SURVIVAL);
                            });
                }
                if (actualEvent != null) {
                    p.sendActionBar(
                            mini.deserialize(actualEvent + ": <green>" + formatTime(timeUntilNextEvent)));
                }

                DeltaUHC.getGame().getBoard(p).updateLines(getProcessedLines());
            });
        }, 20 * 5, 20);

        this.taskId = runTaskTimer.getTaskId();
    }

    private int getTimeUntilEvent(int currentTime, int eventTime) {
        return eventTime > currentTime ? eventTime - currentTime : Integer.MAX_VALUE;
    }

    static Component PVP_DISABLED = mini.deserialize("<red>[⚔] PVP is not enabled.");

    @EventHandler
    public void onPVP(EntityDamageByEntityEvent e) {
        if (e.getDamager().getType() == EntityType.PLAYER && e.getEntityType() == EntityType.PLAYER
                && !DeltaUHC.gameConfig().isPvp()) {
            e.setCancelled(true);
            e.getDamager().sendMessage(PVP_DISABLED);
        }
    }

    @Override
    public void unregisterTasks() {

    }

    /**
     * TODO: Properly implement the teleport as a STAGE and let that stage tell the
     * game when to actually start.
     * 
     * Calculate all the locations needed for teleport, substracting all the
     * locations for each team member, + 1, and preload the chunks and keep them
     * loaded until everyone has been teleported.
     * Teleport players in chunks a x amounts of locations are fully preloaded and
     * have them wait until everyone else teleports.
     * Once everyone is teleported, heal, clear inventory, set gamemode, etc etc,
     * then unregister current stage and register InGamestage.
     */

    public void startTeleport() {
        Set<Player> onlinePlayers = new HashSet<>(Bukkit.getOnlinePlayers());

        // Add initial players to the game configuration
        onlinePlayers.stream().map(Player::getUniqueId).forEach(DeltaUHC.gameConfig()::addPlayer);
        DeltaUHC.gameConfig().setInitialPlayers(DeltaUHC.gameConfig().getPlayersAlive().size());

        // Find safe locations for teleportation
        List<Location> safeLocations = Locations.findSafeLocations(
                Locations.getGameWorld(),
                onlinePlayers.size() + 1,
                DeltaUHC.gameConfig().getRadius(),
                DeltaUHC.gameConfig().getMinDistance());
        Iterator<Location> locationIterator = safeLocations.iterator();

        // Check if the game is a team game
        boolean isTeamsGame = DeltaUHC.gameConfig().getTeamSize() > 1;

        // Teleport players
        if (isTeamsGame) {
            teleportTeamPlayers(onlinePlayers, locationIterator);
        } else {
            teleportSoloPlayers(onlinePlayers, locationIterator);
        }
    }

    private void teleportTeamPlayers(Set<Player> onlinePlayers, Iterator<Location> locationIterator) {
        Map<Team, Location> teamLocations = new HashMap<>();

        for (Player player : onlinePlayers) {
            Team team = DeltaUHC.getGame().getTeamManager().teamByPlayer(player.getUniqueId());

            if (!teamLocations.containsKey(team) && locationIterator.hasNext()) {
                teamLocations.put(team, locationIterator.next());
            }

            Location teleportLocation = teamLocations.get(team);
            if (teleportLocation != null) {
                teleportPlayer(player, teleportLocation);
            } else {
                player.sendMessage(mini.deserialize("<red>Couldn't teleport you to a safe location..."));
            }
        }
    }

    private void teleportSoloPlayers(Set<Player> onlinePlayers, Iterator<Location> locationIterator) {
        for (Player player : onlinePlayers) {
            if (locationIterator.hasNext()) {
                Location teleportLocation = locationIterator.next();
                teleportPlayer(player, teleportLocation);
            } else {
                player.sendMessage(mini.deserialize("<red>Couldn't teleport you to a safe location..."));
            }
        }
    }

    private void teleportPlayer(Player player, Location location) {
        // Spawn an invisible armor stand at the location
        var entity = location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND, SpawnReason.CUSTOM);
        entity.setInvisible(true);
        entity.setInvulnerable(true);

        // Load nearby chunks
        Chunk chunk = entity.getChunk();
        loadNearbyChunks(chunk);

        // Teleport player and add them as a passenger to the armor stand
        player.teleport(location);
        entity.addPassenger(player);

        // Register event listeners for the player
        registerPlayerEvents(player, entity);

        player.sendMessage(mini.deserialize("<yellow>You've been teleported to a safe start location."));
    }

    private void loadNearbyChunks(Chunk chunk) {
        IntStream.rangeClosed(chunk.getX() - 2, chunk.getX() + 2)
                .forEach(x -> IntStream.rangeClosed(chunk.getZ() - 2, chunk.getZ() + 2)
                        .forEach(z -> chunk.getWorld().getChunkAt(x, z).load()));
    }

    private void registerPlayerEvents(Player player, org.bukkit.entity.Entity entity) {
        Listener listener = new Listener() {
            @EventHandler
            public void onPlayerMove(PlayerMoveEvent event) {
                if (event.getPlayer().getUniqueId().equals(player.getUniqueId()) && event.hasExplicitlyChangedBlock()) {
                    HandlerList.unregisterAll(this);
                }
            }

            @EventHandler
            public void onVehicleExit(VehicleExitEvent event) {
                if (event.getExited() instanceof Player exitedPlayer
                        && exitedPlayer.getUniqueId().equals(player.getUniqueId())) {
                    entity.remove();
                    HandlerList.unregisterAll(this);
                }
            }
        };

        Bukkit.getPluginManager().registerEvents(listener, DeltaUHC.getGame());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.getPlayer().setGameMode(GameMode.SPECTATOR);

        DeltaUHC.gameConfig().getPlayersAlive().removeIf(c -> e.getPlayer().getUniqueId().compareTo(c) == 0);
    }

    /**
     * @param taskId the taskId to set
     */
    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

}
