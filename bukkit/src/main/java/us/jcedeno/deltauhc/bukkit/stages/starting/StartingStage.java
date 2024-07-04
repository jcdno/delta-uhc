package us.jcedeno.deltauhc.bukkit.stages.starting;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import us.jcedeno.deltauhc.bukkit.DeltaUHC;
import us.jcedeno.deltauhc.bukkit.locations.Locations;
import us.jcedeno.deltauhc.bukkit.stages.global.AbstractStage;
import us.jcedeno.deltauhc.bukkit.team.models.Team;

/**
 * A game stage between lobby and ingame. Should be responsible for
 * orchestrating the player teleport to safe locations
 * 
 * @author jcedeno
 */
public class StartingStage extends AbstractStage implements Listener {

    @Override
    public void registerTasks() {
        super.registerTasks();
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
        Bukkit.getPluginManager().registerEvents(this, DeltaUHC.getGame());
        // Scatter players

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
        // Teleport completed
        this.unregisterTasks();
        // Register ingame
        DeltaUHC.getGame().getInGameStage().registerTasks();

    }

    @Override
    public void unregisterTasks() {
        super.unregisterTasks();
        HandlerList.unregisterAll(this);
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
                player.sendMessage(miniMessage().deserialize("<red>Couldn't teleport you to a safe location..."));
            }
        }
    }

    private void teleportSoloPlayers(Set<Player> onlinePlayers, Iterator<Location> locationIterator) {
        for (Player player : onlinePlayers) {
            if (locationIterator.hasNext()) {
                Location teleportLocation = locationIterator.next();
                teleportPlayer(player, teleportLocation);
            } else {
                player.sendMessage(miniMessage().deserialize("<red>Couldn't teleport you to a safe location..."));
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

        player.sendMessage(miniMessage().deserialize("<yellow>You've been teleported to a safe start location."));
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

            @EventHandler
            public void cancelDamage(EntityDamageEvent e) {
                if (e.getEntity() instanceof Player p
                        && p.getUniqueId().compareTo(player.getUniqueId()) == 0) {
                    e.setCancelled(true);
                }
            }

            @EventHandler
            public void cancelDamage(PlayerQuitEvent e) {
                if (player.getUniqueId().compareTo(e.getPlayer().getUniqueId()) == 0) {
                    HandlerList.unregisterAll(this);
                }
            }
        };

        Bukkit.getPluginManager().registerEvents(listener, DeltaUHC.getGame());
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockPlaceEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onBreak(PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL)
            e.setCancelled(true);
    }

    @EventHandler
    public void onPLayerMove(PlayerMoveEvent e) {
        if (e.hasChangedBlock()) {
            e.setCancelled(true);
        }

    }

}
