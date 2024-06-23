package us.jcedeno.deltauhc.bukkit.config;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class GameConfig {

    int startPlayers = 8;
    int radius = 2500;
    int minDistance = 250;
    int shrinkStartTime = 60 * 60;
    int shrinkDuration = 60 * 10;
    int radiusFinalSize = 100;
    int pvpTime =  15* 60;
    int healTime = 30;

    volatile boolean pvp = false;
    volatile int currentGameTime = -1;
    volatile List<UUID> playersAlive = new ArrayList<>();
    volatile int initialPlayers = 0;


    public int increaseGameTime(){
        this.currentGameTime++;

        return this.currentGameTime;
    }

    public void addPlayer(UUID uuid){
        this.playersAlive.add(uuid);
    }
    
    
}
