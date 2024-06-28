package us.jcedeno.deltauhc.bukkit.stages.lobby;

import java.util.Arrays;
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

import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import us.jcedeno.deltauhc.bukkit.DeltaUHC;
import us.jcedeno.deltauhc.bukkit.locations.Locations;
import us.jcedeno.deltauhc.bukkit.scenarios.models.BaseScenario;
import us.jcedeno.deltauhc.bukkit.stages.global.AbstractStage;

/***
 * Everything that needs to happen during the lobby /waiting for players period
 * of the Game
 */
public class LobbyStage extends AbstractStage implements Listener {

    public static MiniMessage mini = MiniMessage.miniMessage();

    private Integer taskId;
    private static String LOBBY_BOARD = """
            <#f6edd8>Online: <#cc1a40>%player_count%

            <#f6edd8>Team: <#cc1a40>%teamSize%
            %scenarios%

            @thejcedeno
            """;

    /**
     * 
     * @return Returns the stage's board to be rendered
     */
    private static List<Component> getProcessedLines() {
        int playerCount = Bukkit.getOnlinePlayers().size();

        List<BaseScenario> enabledScenarios = DeltaUHC.getGame().getScenarioManager().enabledScenarios();

        String scenariosString = enabledScenarios.isEmpty()
                ? "<red>No scenarios!</red>"
                : "\n" + enabledScenarios.stream()
                        .map(BaseScenario::name).map(s -> "<white> - " + s + " </white>")
                        .collect(Collectors.joining("\n"));

        String processedBoard = LOBBY_BOARD
                .replaceAll("%player_count%", String.valueOf(playerCount))
                .replace("%scenarios%", scenariosString)
                .replaceAll("%teamSize%", getTeamSizeString(DeltaUHC.gameConfig().getTeamSize()));

        return Arrays.stream(processedBoard.split("\n"))
                .map(mini::deserialize)
                .toList();
    }

    private static String getTeamSizeString(int size) {
        return switch (size) {
            case 0, 1 -> "FFA";
            default -> "To" + size;
        };
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
        /**
         * TODO: Introduce "Loopable stage", an interface that stages can implement to
         * define a common "stage loop".
         * This loop is a Task, in this case BukkitTask, that runs undefinitely until
         * the stage is unregistered.
         * The loop will take in as input whether it is async or not, the period, and
         * the initialDelay.
         */
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
