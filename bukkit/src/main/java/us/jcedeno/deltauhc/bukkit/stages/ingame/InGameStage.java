package us.jcedeno.deltauhc.bukkit.stages.ingame;

import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Instrument;
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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.ChatColor;
import us.jcedeno.deltauhc.bukkit.DeltaUHC;
import us.jcedeno.deltauhc.bukkit.locations.Locations;
import us.jcedeno.deltauhc.bukkit.stages.global.AbstractStage;
import us.jcedeno.deltauhc.bukkit.stages.ingame.listeners.ScatterListeners;

import static us.jcedeno.deltauhc.bukkit.common.utils.StringUtils.formatTime;

public class InGameStage extends AbstractStage implements Listener {
    public static MiniMessage mini = MiniMessage.miniMessage();
    public ScatterListeners scatterListeners = new ScatterListeners();
    private Integer taskId;

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
                        mini.deserialize("<yellow>[!!] The world border will begging to shrink and it will take <white>"
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
            var currentBorder = Locations.getGameWorld().getWorldBorder().getSize() / 2;
            var alive = DeltaUHC.gameConfig().getPlayersAlive().size();
            var initial = DeltaUHC.gameConfig().getInitialPlayers();

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
                p.sendActionBar(mini.deserialize("Time: <green>" + formatTime(time) + "</green>"));
                DeltaUHC.getGame().getBoard(p).updateLines(
                        ChatColor.WHITE + "Time: " + ChatColor.GOLD + formatTime(time),
                        "\n",
                        ChatColor.WHITE + "Alive: " + ChatColor.GOLD + alive + ChatColor.GRAY + "/" + ChatColor.YELLOW
                                + initial,
                        ChatColor.WHITE + "Border: " + ChatColor.GOLD + currentBorder,
                        "\n",
                        "@thejcedeno");
            });
        }, 20 * 5, 20);

        this.taskId = runTaskTimer.getTaskId();
    }

    @Override
    public void unregisterTasks() {

    }

    public void startTeleport() {
        var online = Bukkit.getOnlinePlayers();
        // Add initial player
        online.stream().map(Player::getUniqueId).forEach(DeltaUHC.gameConfig()::addPlayer);
        DeltaUHC.gameConfig().setInitialPlayers(DeltaUHC.gameConfig().getPlayersAlive().size());

        var locs = Locations.findSafeLocations(Locations.getGameWorld(), online.size() + 1,
                DeltaUHC.gameConfig().getRadius(), DeltaUHC.gameConfig().getMinDistance());
        var iterator = locs.iterator();

        online.forEach(p -> {
            if (iterator.hasNext()) {
                var loc = iterator.next();
                p.teleport(loc);
                var entity = loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND, SpawnReason.CUSTOM);
                entity.setInvisible(true);

                entity.addPassenger(p);

                Bukkit.getPluginManager().registerEvents(new Listener() {
                    @EventHandler
                    public void exitVehicle(VehicleExitEvent e) {
                        if (e.getExited() instanceof Player pl && pl.getUniqueId().compareTo(p.getUniqueId()) == 0) {
                            entity.remove();
                        }
                    }

                    @EventHandler
                    public void playerMove(PlayerMoveEvent e) {
                        if (e.getPlayer().getUniqueId().compareTo(p.getUniqueId()) == 0
                                && e.hasExplicitlyChangedBlock()) {
                            HandlerList.unregisterAll(this);
                        }
                    }

                }, DeltaUHC.getGame());

                p.sendMessage(mini.deserialize("<yellow>You've been teleported to a safe start location."));
            } else
                p.sendMessage(mini.deserialize("<red>Couldn't teleport you to a safe location..."));
        });

    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.getPlayer().setHealth(e.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        e.getPlayer().setGameMode(GameMode.SPECTATOR);
    }

    /**
     * @param taskId the taskId to set
     */
    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

}
