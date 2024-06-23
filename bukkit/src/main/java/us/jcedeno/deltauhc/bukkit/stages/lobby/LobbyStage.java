package us.jcedeno.deltauhc.bukkit.stages.lobby;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import us.jcedeno.deltauhc.bukkit.DeltaUHC;
import us.jcedeno.deltauhc.bukkit.locations.Locations;

/***
 * Everything that needs to happen during the lobby /waiting for players period
 * of the Game
 */
public class LobbyStage implements Listener {

    public static MiniMessage mini =  MiniMessage.miniMessage();

    private Integer taskId;

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        var player = e.getPlayer();

        player.sendMessage(Component.text("Welcome!"));
        player.teleport(Locations.getLobbySpawn());

        if(Bukkit.getOnlinePlayers().size() >= DeltaUHC.gameConfig().getStartPlayers() ){
            Bukkit.broadcast(mini.deserialize("<yellow>Player threshold to start has been met!"));
            //Auto start the game if set to, otherwise wait for confirmation
        }

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
    public void onEntityDamage(EntityDamageEvent e){
        e.setCancelled(true);
    }

    /**
     * Method to be called to register the behavior of this class. In this case the timer and events.
     */
    public void registerTasks(){
        BukkitTask runTaskTimer = Bukkit.getScheduler().runTaskTimer(DeltaUHC.getGame(), ()->{
            var sp = DeltaUHC.gameConfig().getStartPlayers();
            final var online = Bukkit.getOnlinePlayers().size();
            

            Bukkit.getOnlinePlayers().stream().forEach((p)->{
                p.sendActionBar(mini.deserialize("<green>" + online + "/" + sp + " players needed to start!"));
            });
        }, 0, 0);
        this.taskId = runTaskTimer.getTaskId();        

        Bukkit.getPluginManager().registerEvents(this, DeltaUHC.getGame());
    }

    /**
     * Method to be called when you'd like to disable the lobby stage and move the game to a different stage.
     * 
     * Only to be called if {@link #registerTasks()} has been called.
     */
    public void unregisterTasks(){
        Bukkit.getScheduler().cancelTask(this.taskId);
        HandlerList.unregisterAll(this);
    }
    

}