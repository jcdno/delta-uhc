package us.jcedeno.deltauhc.bukkit.stages.ingame;

import static us.jcedeno.deltauhc.bukkit.common.utils.StringUtils.formatTime;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import us.jcedeno.deltauhc.bukkit.DeltaUHC;
import us.jcedeno.deltauhc.bukkit.locations.Locations;
import us.jcedeno.deltauhc.bukkit.stages.global.AbstractStage;
import us.jcedeno.deltauhc.bukkit.stages.ingame.tasks.GameLoop;

public class InGameStage extends AbstractStage implements Listener {
    public static MiniMessage mini = MiniMessage.miniMessage();
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

    static Component PVP_DISABLED = mini.deserialize("<red>[⚔] PVP is not enabled.");

    /**
     * 
     * @return Returns the stage's board to be rendered
     */
    public List<Component> getProcessedLines() {
        var config = DeltaUHC.gameConfig();

        String processedBoard = GAME_BOARD
                .replace("%formattedGameTime%", formatTime(config.getCurrentGameTime()))
                .replace("%playersAlive%", "" + config.getPlayersAlive().size())
                .replace("%initialPlayers%", "" + config.getInitialPlayers())
                .replace("%currentBorder%",
                        "" + String.format("%.2f", Locations.getGameWorld().getWorldBorder().getSize() / 2));

        return Arrays.stream(processedBoard.split("\n")).map(m -> mini.deserialize(m)).toList();
    }

    /**
     * Method to be called when the stage gets registered, presummably once starting
     * stage has completed.
     */
    @Override
    public void registerTasks() {
        super.registerTasks();
        Locations.getGameWorld().setDifficulty(Difficulty.HARD);
        // Register this class as a listener
        Bukkit.getPluginManager().registerEvents(this, DeltaUHC.getGame());

        // Start the game timer
        BukkitTask runTaskTimer = Bukkit.getScheduler().runTaskTimerAsynchronously(DeltaUHC.getGame(),
                new GameLoop(DeltaUHC.gameConfig(), this), 0, 20);

        // Send initial game items
        Bukkit.getScheduler().runTask(DeltaUHC.getGame(),
                () -> {
                    Bukkit.getOnlinePlayers().forEach(p -> {
                        p.sendMessage(mini.deserialize("<green>The game has begun!"));
                        p.clearActivePotionEffects();
                        p.getInventory().clear();
                        p.getInventory().addItem(ItemStack.of(Material.COOKED_BEEF, 8));
                        p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                        p.setGameMode(GameMode.SURVIVAL);

                    });
                });

        this.taskId = runTaskTimer.getTaskId();
    }

    @Override
    public void unregisterTasks() {
        super.unregisterTasks();
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTask(this.taskId);
    }

    @EventHandler
    public void onPVP(EntityDamageByEntityEvent e) {
        if (e.getDamager().getType() == EntityType.PLAYER && e.getEntityType() == EntityType.PLAYER
                && !DeltaUHC.gameConfig().isPvp()) {
            e.setCancelled(true);
            e.getDamager().sendMessage(PVP_DISABLED);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.getPlayer().setGameMode(GameMode.SPECTATOR);

        DeltaUHC.gameConfig().getPlayersAlive().removeIf(c -> e.getPlayer().getUniqueId().compareTo(c) == 0);
    }

}
