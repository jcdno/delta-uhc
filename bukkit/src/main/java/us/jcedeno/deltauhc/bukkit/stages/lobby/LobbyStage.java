package us.jcedeno.deltauhc.bukkit.stages.lobby;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.ChatColor;
import us.jcedeno.deltauhc.bukkit.DeltaUHC;
import us.jcedeno.deltauhc.bukkit.config.GameConfig;
import us.jcedeno.deltauhc.bukkit.locations.Locations;
import us.jcedeno.deltauhc.bukkit.stages.global.AbstractStage;

/***
 * Everything that needs to happen during the lobby /waiting for players period
 * of the Game
 */
public class LobbyStage extends AbstractStage implements Listener {

    public static MiniMessage mini = MiniMessage.miniMessage();

    private Integer taskId;
    private static String LOBBY_BOARD = """
            %primary%Online: %secondary%%player_count%

            %config%

            %secondary%@thejcedeno
            """;

    public static List<String> formatConfigWithColors(GameConfig config, String primaryColor, String secondaryColor) {
        List<String> result = new ArrayList<>();
        Field[] fields = GameConfig.class.getDeclaredFields();

        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object value = field.get(config);
                if (value instanceof List) {
                    value = ((List<?>) value).size(); // For List<UUID> playersAlive, we just display the size
                }
                result.add(String.format("%s%s: %s%s", primaryColor, field.getName(), secondaryColor, value));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 
     * @return Returns the stage's board to be rendered
     */
    private static String[] getProcessedLines() {
        var config = DeltaUHC.gameConfig();
        String primaryColor = ChatColor.of("#f6edd8") + "";
        String secondaryColor = ChatColor.of("#cc1a40") + "";

        List<String> configLines = formatConfigWithColors(config, primaryColor, secondaryColor);

        StringBuilder configReplacement = new StringBuilder();
        for (String line : configLines) {
            configReplacement.append(line).append("\n");
        }

        String processedBoard = LOBBY_BOARD
                .replace("%player_count%", "" + Bukkit.getOnlinePlayers().size())
                .replace("%primary%", primaryColor)
                .replace("%secondary%", secondaryColor);

        // Replace %config% after replacing other placeholders to ensure %secondary% in
        // @thejcedeno line gets replaced correctly
        processedBoard = processedBoard.replace("%config%", configReplacement.toString().trim());

        return processedBoard.split("\n");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        var player = e.getPlayer();

        player.sendMessage(mini.deserialize("<rainbow>Welcome to delta-uhc!"));
        player.teleport(Locations.getLobbySpawn());
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        player.setExp(0);
        player.setLevel(0);
        player.getInventory().clear();
        player.clearActivePotionEffects();
        player.setFoodLevel(20);

        DeltaUHC.getGame().getBoard(player).updateLines(getProcessedLines());

        if (Bukkit.getOnlinePlayers().size() >= DeltaUHC.gameConfig().getStartPlayers()) {
            Bukkit.broadcast(mini.deserialize("<yellow>Player threshold to start has been met!"));
            // Auto start the game if set to, otherwise wait for confirmation
        }

    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.hasExplicitlyChangedBlock() && e.getTo().getY() < 20) {
            e.getPlayer().teleport(Locations.getLobbySpawn());
        }
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
    public void onEntityDamage(EntityDamageEvent e) {
        e.setCancelled(true);
    }

    /**
     * Method to be called to register the behavior of this class. In this case the
     * timer and events.
     */
    public void registerTasks() {
        BukkitTask runTaskTimer = Bukkit.getScheduler().runTaskTimer(DeltaUHC.getGame(), () -> {
            var sp = DeltaUHC.gameConfig().getStartPlayers();
            final var online = Bukkit.getOnlinePlayers().size();
            var lines = getProcessedLines();

            Bukkit.getOnlinePlayers().stream().forEach((p) -> {
                p.sendActionBar(mini.deserialize("<green>" + online + "/" + sp + " players needed to start!"));
                DeltaUHC.getGame().getBoard(p).updateLines(lines);
            });
        }, 0, 0);
        this.taskId = runTaskTimer.getTaskId();

        Bukkit.getPluginManager().registerEvents(this, DeltaUHC.getGame());
    }

    /**
     * Method to be called when you'd like to disable the lobby stage and move the
     * game to a different stage.
     * 
     * Only to be called if {@link #registerTasks()} has been called.
     */
    public void unregisterTasks() {
        Bukkit.getScheduler().cancelTask(this.taskId);
        HandlerList.unregisterAll(this);
    }

}
