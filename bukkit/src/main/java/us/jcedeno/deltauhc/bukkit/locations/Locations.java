package us.jcedeno.deltauhc.bukkit.locations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class Locations {

    /**
     * Gets the lobby spawn location.
     * 
     * @return
     */
    public static Location getLobbySpawn() {

        return Bukkit.getWorlds().get(0).getHighestBlockAt(0, 0).getLocation().add(0.5, 1.5, 0.5);
    }

    /**
     * Gets the game world.
     * 
     * @return
     */
    public static World getGameWorld() {

        return Bukkit.getWorlds().get(0);
    }

    private static final Random RANDOM = new Random();

    public static List<Location> findSafeLocations(World world, int numberOfLocations, double radius,
            double minDistance) {
        List<Location> safeLocations = new ArrayList<>();

        int attempts = 0;
        while (safeLocations.size() < numberOfLocations && attempts < numberOfLocations * 10) {
            Location randomLocation = getRandomLocation(world, radius);
            if (isSafeLocation(randomLocation) && isFarEnough(safeLocations, randomLocation, minDistance)) {
                safeLocations.add(randomLocation);
            }
            attempts++;
        }

        return safeLocations;
    }

    private static Location getRandomLocation(World world, double radius) {
        double x = (RANDOM.nextDouble() * 2 - 1) * radius;
        double z = (RANDOM.nextDouble() * 2 - 1) * radius;
        int y = world.getHighestBlockYAt((int) x, (int) z);

        return new Location(world, x, y, z);
    }

    private static boolean isSafeLocation(Location location) {
        Material blockType = location.getBlock().getType();
        return blockType.isSolid() && !blockType.equals(Material.WATER) && !blockType.equals(Material.LAVA);
    }

    private static boolean isFarEnough(List<Location> locations, Location newLocation, double minDistance) {
        for (Location location : locations) {
            if (location.distance(newLocation) < minDistance) {
                return false;
            }
        }
        return true;
    }

}
