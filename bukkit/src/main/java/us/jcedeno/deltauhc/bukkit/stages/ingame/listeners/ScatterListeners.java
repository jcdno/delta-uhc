package us.jcedeno.deltauhc.bukkit.stages.ingame.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class ScatterListeners implements Listener {

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        e.setCancelled(true);
    }
    
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent e){
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
    public void onPLayerMove(PlayerMoveEvent e){
        if(e.hasChangedBlock()){
            e.setCancelled(true);
        }

    }

    
}
