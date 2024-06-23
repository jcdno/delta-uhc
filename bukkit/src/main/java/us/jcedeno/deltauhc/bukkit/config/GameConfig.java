package us.jcedeno.deltauhc.bukkit.config;

import lombok.Data;

@Data
public class GameConfig {

    int startPlayers = 8;
    int radius = 2500;
    int minDistance = 250;
    int shrinkStartTime = 60 * 60;
    int shrinkDuration = 60 * 10;
    int radiusFinalSize = 100;

    volatile int currentGameTime =0;


    public int increaseGameTime(){
        this.currentGameTime++;

        return this.currentGameTime;
    }
    
}
