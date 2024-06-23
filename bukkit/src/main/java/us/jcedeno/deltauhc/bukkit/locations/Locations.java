package us.jcedeno.deltauhc.bukkit.locations;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class Locations {
    
    /**
     * Gets the lobby spawn location.
     * @return
     */
    public static Location getLobbySpawn(){

        return Bukkit.getWorlds().get(0).getHighestBlockAt(0, 0).getLocation().add(0.5, 1.5, 0.5);
    }
    
}
