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
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import net.kyori.adventure.text.minimessage.MiniMessage;
import us.jcedeno.deltauhc.bukkit.DeltaUHC;
import us.jcedeno.deltauhc.bukkit.common.GameStage;
import us.jcedeno.deltauhc.bukkit.locations.Locations;
import us.jcedeno.deltauhc.bukkit.stages.ingame.listeners.ScatterListeners;

public class InGameStage implements Listener, GameStage {
    public static MiniMessage mini = MiniMessage.miniMessage();
    public ScatterListeners scatterListeners = new ScatterListeners();
    public boolean registered = false;
    private Integer taskId;

    @Override
    public void registerTasks() {
        if (registered) {
            throw new RuntimeException("Cannot register an already registered stage.");
        }
        this.registered = true;
        // Unregister lobby
        DeltaUHC.getGame().getLobbyStage().unregisterTasks();
        // Set world border
        var border = Locations.getGameWorld().getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(DeltaUHC.gameConfig().getRadius());
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
        }, 20 * 15);
        // Register this class as a listener
        Bukkit.getPluginManager().registerEvents(this, DeltaUHC.getGame());
        // TODO: Register a task that handles the whole start sequence and waiting for start stage

        final AtomicBoolean firstTime = new AtomicBoolean(true);
        // wait for everyone to start

        // Start the game timer
        BukkitTask runTaskTimer = Bukkit.getScheduler().runTaskTimerAsynchronously(DeltaUHC.getGame(), () -> {
            var time = DeltaUHC.gameConfig().increaseGameTime();
            // Do second driven game loop here?

            if (time == DeltaUHC.gameConfig().getShrinkStartTime()) {
                // Start border shrink
                Locations.getGameWorld().getWorldBorder().setSize(DeltaUHC.gameConfig().getRadiusFinalSize(),
                        DeltaUHC.gameConfig().getShrinkDuration());
                Bukkit.broadcast(
                        mini.deserialize("<yellow>[⚠️] The world border will begging to shirnk and it will take <white>"
                                + formatTime(DeltaUHC.gameConfig().getShrinkDuration())
                                + "<white> to complete shrinking down to a radius of <white>"
                                + DeltaUHC.gameConfig().getRadiusFinalSize() + "</white> blocks!</yellow>"));
                Bukkit.getOnlinePlayers()
                        .forEach(p -> p.playNote(p.getLocation(), Instrument.CHIME, Note.flat(1, Tone.A)));
            }

            Bukkit.getOnlinePlayers().stream().forEach((p) -> {
                if (firstTime.get()) {
                    firstTime.set(false);
                    p.sendMessage(mini.deserialize("<green>The game has begun!"));
                    Bukkit.getScheduler().runTask(DeltaUHC.getGame(),
                            () -> p.getInventory().addItem(ItemStack.of(Material.COOKED_BEEF, 8)));
                }
                p.sendActionBar(mini.deserialize("Time: <green>" + formatTime(time) + "</green>"));
            });
        }, 20 * 5, 20);

        this.taskId = runTaskTimer.getTaskId();
    }

    @Override
    public void unregisterTasks() {
        if (!registered) {
            throw new RuntimeException("Cannot unregister a stage that hasn't been registered.");
        }
        this.registered = false;
    }

    public void startTeleport() {
        var online = Bukkit.getOnlinePlayers();

        var locs = Locations.findSafeLocations(Locations.getGameWorld(), online.size() + 1,
                DeltaUHC.gameConfig().getRadius(), DeltaUHC.gameConfig().getMinDistance());
        var iterator = locs.iterator();

        online.forEach(p -> {
            if (iterator.hasNext()) {
                p.teleport(iterator.next());
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

    public static String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int remainingSeconds = seconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
        } else {
            return String.format("%02d:%02d", minutes, remainingSeconds);
        }
    }

    @Override
    public boolean registered() {
        return this.registered;
    }

}
